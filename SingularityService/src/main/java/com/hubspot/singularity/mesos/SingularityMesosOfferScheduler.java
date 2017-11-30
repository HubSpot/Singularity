package com.hubspot.singularity.mesos;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.apache.mesos.v1.Protos.Offer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.helpers.MesosUtils;
import com.hubspot.mesos.Resources;
import com.hubspot.singularity.helpers.SingularityMesosTaskHolder;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityDeployStatistics;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularitySlaveUsage.ResourceUsageType;
import com.hubspot.singularity.SingularitySlaveUsageWithId;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.SlaveMatchState;
import com.hubspot.singularity.config.CustomExecutorConfiguration;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.DisasterManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.UsageManager;
import com.hubspot.singularity.scheduler.SingularityLeaderCache;
import com.hubspot.singularity.scheduler.SingularityScheduler;

@Singleton
public class SingularityMesosOfferScheduler {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityMesosOfferScheduler.class);

  private final Resources defaultResources;
  private final Resources defaultCustomExecutorResources;
  private final TaskManager taskManager;
  private final SingularityMesosTaskPrioritizer taskPrioritizer;
  private final SingularityScheduler scheduler;
  private final SingularityConfiguration configuration;
  private final SingularityMesosTaskBuilder mesosTaskBuilder;
  private final SingularitySlaveAndRackManager slaveAndRackManager;
  private final SingularitySlaveAndRackHelper slaveAndRackHelper;
  private final SingularityTaskSizeOptimizer taskSizeOptimizer;
  private final DisasterManager disasterManager;
  private final UsageManager usageManager;
  private final DeployManager deployManager;


  private final SingularityLeaderCache leaderCache;

  @Inject
  public SingularityMesosOfferScheduler(MesosConfiguration mesosConfiguration,
                                        CustomExecutorConfiguration customExecutorConfiguration,
                                        TaskManager taskManager,
                                        SingularityMesosTaskPrioritizer taskPrioritizer,
                                        SingularityScheduler scheduler,
                                        SingularityConfiguration configuration,
                                        SingularityMesosTaskBuilder mesosTaskBuilder,
                                        SingularitySlaveAndRackManager slaveAndRackManager,
                                        SingularityTaskSizeOptimizer taskSizeOptimizer,
                                        SingularitySlaveAndRackHelper slaveAndRackHelper,
                                        SingularityLeaderCache leaderCache,
                                        DisasterManager disasterManager,
                                        UsageManager usageManager,
                                        DeployManager deployManager) {
    this.defaultResources = new Resources(mesosConfiguration.getDefaultCpus(), mesosConfiguration.getDefaultMemory(), 0, mesosConfiguration.getDefaultDisk());
    this.defaultCustomExecutorResources = new Resources(customExecutorConfiguration.getNumCpus(), customExecutorConfiguration.getMemoryMb(), 0, customExecutorConfiguration.getDiskMb());
    this.taskManager = taskManager;
    this.scheduler = scheduler;
    this.configuration = configuration;
    this.mesosTaskBuilder = mesosTaskBuilder;
    this.slaveAndRackManager = slaveAndRackManager;
    this.taskSizeOptimizer = taskSizeOptimizer;
    this.leaderCache = leaderCache;
    this.slaveAndRackHelper = slaveAndRackHelper;
    this.disasterManager = disasterManager;
    this.taskPrioritizer = taskPrioritizer;
    this.usageManager = usageManager;
    this.deployManager = deployManager;
  }

  public List<SingularityOfferHolder> checkOffers(final Collection<Offer> offers) {
    for (SingularityPendingTaskId taskId : taskManager.getPendingTasksMarkedForDeletion()) {
      taskManager.deletePendingTask(taskId);
    }

    boolean useTaskCredits = disasterManager.isTaskCreditEnabled();
    int taskCredits = useTaskCredits ? disasterManager.getUpdatedCreditCount() : -1;

    scheduler.checkForDecomissions();
    scheduler.drainPendingQueue();

    final Map<String, SingularityTaskRequestHolder> pendingTaskIdToTaskRequest = getDueTaskRequestHolders();

    final int numDueTasks = pendingTaskIdToTaskRequest.size();

    if (offers.isEmpty()) {
      LOG.debug("No offers to check");
      return Collections.emptyList();
    }

    final List<SingularityOfferHolder> offerHolders = offers.stream()
        .collect(Collectors.groupingBy((o) -> o.getAgentId().getValue()))
        .entrySet().stream()
        .filter((e) -> e.getValue().size() > 0)
        .map((e) -> {
          List<Offer> offersList = e.getValue();
          String slaveId = e.getKey();
          return new SingularityOfferHolder(
              offersList,
              numDueTasks,
              slaveAndRackHelper.getRackIdOrDefault(offersList.get(0)),
              slaveId,
              offersList.get(0).getHostname(),
              slaveAndRackHelper.getTextAttributes(offersList.get(0)),
              slaveAndRackHelper.getReservedSlaveAttributes(offersList.get(0)));
        })
        .collect(Collectors.toList());

    final Map<String, Map<String, Integer>> tasksPerOfferPerRequest = new HashMap<>();

    boolean addedTaskInLastLoop = true;
    int tasksScheduled = 0;
    final List<SingularitySlaveUsageWithId> currentSlaveUsages = usageManager.getCurrentSlaveUsages(offerHolders.stream().map(SingularityOfferHolder::getSlaveId).collect(Collectors.toList()));


    while (!pendingTaskIdToTaskRequest.isEmpty() && addedTaskInLastLoop && canScheduleAdditionalTasks(taskCredits)) {
      addedTaskInLastLoop = false;

      for (Iterator<SingularityTaskRequestHolder> iterator = pendingTaskIdToTaskRequest.values().iterator(); iterator.hasNext();) {
        SingularityTaskRequestHolder taskRequestHolder = iterator.next();

        Map<SingularityOfferHolder, Double> scorePerOffer = new HashMap<>();

        for (SingularityOfferHolder offerHolder : offerHolders) {

          if (configuration.getMaxTasksPerOffer() > 0 && offerHolder.getAcceptedTasks().size() >= configuration.getMaxTasksPerOffer()) {
            LOG.debug("Offer holder for slave {} is full ({}) - skipping", offerHolder.getHostname(), offerHolder.getAcceptedTasks().size());
            continue;
          }

          Optional<SingularitySlaveUsageWithId> maybeSlaveUsage = getSlaveUsage(currentSlaveUsages, offerHolder.getSlaveId());

          double score = score(offerHolder, tasksPerOfferPerRequest, taskRequestHolder, maybeSlaveUsage);

          LOG.trace("Scored {} | Task {} | Offer - mem {} - cpu {} | Slave {} | maybeSlaveUsage - {}", score, taskRequestHolder.getTaskRequest().getPendingTask().getPendingTaskId().getId(),
              MesosUtils.getMemory(offerHolder.getCurrentResources(), Optional.absent()), MesosUtils.getNumCpus(offerHolder.getCurrentResources(), Optional.absent()), offerHolder.getHostname(), maybeSlaveUsage);

          if (score != 0) {
            scorePerOffer.put(offerHolder, score);
          }
        }

        if (!scorePerOffer.isEmpty()) {
          SingularityOfferHolder bestOffer = Collections.max(scorePerOffer.entrySet(), Map.Entry.comparingByValue()).getKey();
          LOG.info("Best offer {}/1 is on {}", scorePerOffer.get(bestOffer), bestOffer.getSanitizedHost());

          SingularityMesosTaskHolder taskHolder = acceptTask(bestOffer, tasksPerOfferPerRequest, taskRequestHolder);

          tasksScheduled++;
          if (useTaskCredits) {
            taskCredits--;
            LOG.debug("Remaining task credits: {}", taskCredits);
          }
          bestOffer.addMatchedTask(taskHolder);
          addedTaskInLastLoop = true;
          iterator.remove();
          if (useTaskCredits && taskCredits == 0) {
            LOG.info("Used all available task credits, not scheduling any more tasks");
            break;
          }
        }
      }
    }

    if (useTaskCredits) {
      disasterManager.saveTaskCreditCount(taskCredits);
    }

    LOG.info("{} tasks scheduled, {} tasks remaining after examining {} offers", tasksScheduled, numDueTasks - tasksScheduled, offers.size());

    return offerHolders;
  }

  private double getNormalizedWeight(ResourceUsageType type) {
    double freeCpuWeight = configuration.getFreeCpuWeightForOffer();
    double freeMemWeight = configuration.getFreeMemWeightForOffer();
    double usedCpuWeight = configuration.getLongRunningUsedCpuWeightForOffer();
    double usedMemWeight = configuration.getLongRunningUsedMemWeightForOffer();

    switch (type) {
      case CPU_FREE:
        return freeCpuWeight + freeMemWeight != 1 ? freeCpuWeight / (freeCpuWeight + freeMemWeight) : freeCpuWeight;
      case MEMORY_BYTES_FREE:
        return freeCpuWeight + freeMemWeight != 1 ? freeMemWeight / (freeCpuWeight + freeMemWeight) : freeMemWeight;
      case CPU_USED:
        return usedCpuWeight + usedMemWeight != 1 ? usedCpuWeight / (usedCpuWeight + usedMemWeight) : usedCpuWeight;
      case MEMORY_BYTES_USED:
        return usedCpuWeight + usedMemWeight != 1 ? usedMemWeight / (usedCpuWeight + usedMemWeight) : usedMemWeight;
      default:
        LOG.error("Invalid ResourceUsageType {}", type);
        return 0;
    }
  }

  private Map<String, SingularityTaskRequestHolder> getDueTaskRequestHolders() {
    final List<SingularityTaskRequest> taskRequests = taskPrioritizer.getSortedDueTasks(scheduler.getDueTasks());

    for (SingularityTaskRequest taskRequest : taskRequests) {
      LOG.trace("Task {} is due", taskRequest.getPendingTask().getPendingTaskId());
    }

    taskPrioritizer.removeTasksAffectedByPriorityFreeze(taskRequests);

    final Map<String, SingularityTaskRequestHolder> taskRequestHolders = new HashMap<>(taskRequests.size());

    for (SingularityTaskRequest taskRequest : taskRequests) {
      taskRequestHolders.put(taskRequest.getPendingTask().getPendingTaskId().getId(), new SingularityTaskRequestHolder(taskRequest, defaultResources, defaultCustomExecutorResources));
    }

    return taskRequestHolders;
  }

  private boolean canScheduleAdditionalTasks(int taskCredits) {
    return taskCredits == -1 || taskCredits > 0;
  }

  private Optional<SingularitySlaveUsageWithId> getSlaveUsage(List<SingularitySlaveUsageWithId> slaveUsages, String slaveId) {
    List<SingularitySlaveUsageWithId> filteredSlaveUsages = slaveUsages.stream().filter(u -> u.getSlaveId().equals(slaveId)).collect(Collectors.toList());

    return filteredSlaveUsages.size() == 1 ? Optional.of(filteredSlaveUsages.get(0)) : Optional.absent();
  }

  private double score(SingularityOfferHolder offerHolder, Map<String, Map<String, Integer>> tasksPerOfferHostPerRequest,
                       SingularityTaskRequestHolder taskRequestHolder, Optional<SingularitySlaveUsageWithId> maybeSlaveUsage) {

    final SingularityTaskRequest taskRequest = taskRequestHolder.getTaskRequest();
    final SingularityPendingTaskId pendingTaskId = taskRequest.getPendingTask().getPendingTaskId();

    if (offerHolder.hasRejectedPendingTaskAlready(pendingTaskId)) {
      return 0;
    }

    if (tooManyTasksPerOfferHostForRequest(tasksPerOfferHostPerRequest, offerHolder.getHostname(), taskRequestHolder.getTaskRequest())) {
      LOG.debug("Skipping task request for request id {}, too many tasks already scheduled using offer {}", taskRequest.getRequest().getId(), offerHolder.getHostname());
      return 0;
    }

    if (isTooManyInstancesForRequest(taskRequest)) {
      LOG.debug("Skipping pending task {}, too many instances already running", pendingTaskId);
      return 0;
    }

    if (LOG.isTraceEnabled()) {
      LOG.trace("Attempting to match task {} resources {} with required role '{}' ({} for task + {} for executor) with remaining offer resources {}",
          pendingTaskId, taskRequestHolder.getTotalResources(), taskRequest.getRequest().getRequiredRole().or("*"),
          taskRequestHolder.getTaskResources(), taskRequestHolder.getExecutorResources(), MesosUtils.formatForLogging(offerHolder.getCurrentResources()));
    }

    final boolean matchesResources = MesosUtils.doesOfferMatchResources(taskRequest.getRequest().getRequiredRole(),
        taskRequestHolder.getTotalResources(), offerHolder.getCurrentResources(), taskRequestHolder.getRequestedPorts());
    final SlaveMatchState slaveMatchState = slaveAndRackManager.doesOfferMatch(offerHolder, taskRequest);

    if (matchesResources && slaveMatchState.isMatchAllowed()) {
      return score(offerHolder.getHostname(), taskRequest, maybeSlaveUsage);
    } else {
      offerHolder.addRejectedTask(pendingTaskId);

      if (LOG.isTraceEnabled()) {
        LOG.trace("Ignoring offer on host {} with roles {} on {} for task {}; matched resources: {}, slave match state: {}", offerHolder.getHostname(),
            offerHolder.getRoles(), offerHolder.getHostname(), pendingTaskId, matchesResources, slaveMatchState);
      }
    }

    return 0;
  }

  @VisibleForTesting
  double score(String hostname, SingularityTaskRequest taskRequest, Optional<SingularitySlaveUsageWithId> maybeSlaveUsage) {
    if (isMissingUsageData(maybeSlaveUsage)) {
      LOG.info("Slave {} has missing usage data ({}). Will default to {}", hostname, maybeSlaveUsage, configuration.getDefaultOfferScoreForMissingUsage());
      return configuration.getDefaultOfferScoreForMissingUsage();
    }

    SingularitySlaveUsageWithId slaveUsage = maybeSlaveUsage.get();
    Map<ResourceUsageType, Number> longRunningTasksUsage = slaveUsage.getLongRunningTasksUsage();

    double longRunningCpusUsedScore = longRunningTasksUsage.get(ResourceUsageType.CPU_USED).doubleValue() / slaveUsage.getCpusTotal().get();
    double longRunningMemUsedScore = ((double) longRunningTasksUsage.get(ResourceUsageType.MEMORY_BYTES_USED).longValue() / slaveUsage.getMemoryBytesTotal().get());

    double cpusFreeScore = 1 - (slaveUsage.getCpusReserved() / slaveUsage.getCpusTotal().get());
    double memFreeScore = 1 - ((double) slaveUsage.getMemoryMbReserved() / slaveUsage.getMemoryMbTotal().get());

    return isLongRunning(taskRequest) ? scoreLongRunningTask(longRunningMemUsedScore, memFreeScore, longRunningCpusUsedScore, cpusFreeScore)
        : scoreNonLongRunningTask(taskRequest, longRunningMemUsedScore, memFreeScore, longRunningCpusUsedScore, cpusFreeScore);
  }

  private boolean isMissingUsageData(Optional<SingularitySlaveUsageWithId> maybeSlaveUsage) {
    return !maybeSlaveUsage.isPresent() ||
        !maybeSlaveUsage.get().getCpusTotal().isPresent() || !maybeSlaveUsage.get().getMemoryMbTotal().isPresent() ||
        maybeSlaveUsage.get().getLongRunningTasksUsage() == null ||
        !maybeSlaveUsage.get().getLongRunningTasksUsage().containsKey(ResourceUsageType.CPU_USED) ||
        !maybeSlaveUsage.get().getLongRunningTasksUsage().containsKey(ResourceUsageType.MEMORY_BYTES_USED);
  }

  private boolean isLongRunning(SingularityTaskRequest taskRequest) {
    return taskRequest.getRequest().getRequestType().isLongRunning();
  }

  private double scoreLongRunningTask(double longRunningMemUsedScore, double memFreeScore, double longRunningCpusUsedScore, double cpusFreeScore) {
    // unused, reserved resources improve score
    return calculateScore(1 - longRunningMemUsedScore, memFreeScore, 1 - longRunningCpusUsedScore, cpusFreeScore, 0.50, 0.50);
  }

  private double scoreNonLongRunningTask(SingularityTaskRequest taskRequest, double longRunningMemUsedScore, double memFreeScore, double longRunningCpusUsedScore, double cpusFreeScore) {
    Optional<SingularityDeployStatistics> statistics = deployManager.getDeployStatistics(taskRequest.getRequest().getId(), taskRequest.getDeploy().getId());
    final double epsilon = 0.0001;

    double freeResourceWeight = 0.75;
    double usedResourceWeight = 0.25;

    if (statistics.isPresent() && statistics.get().getAverageRuntimeMillis().isPresent()) {
      final double maxNonLongRunningUsedResourceWeight = configuration.getMaxNonLongRunningUsedResourceWeight();
      usedResourceWeight = Math.min((double) TimeUnit.MILLISECONDS.toSeconds(statistics.get().getAverageRuntimeMillis().get()) / configuration.getConsiderNonLongRunningTaskLongRunningAfterRunningForSeconds(), 1) * maxNonLongRunningUsedResourceWeight;

      if (Math.abs(usedResourceWeight - maxNonLongRunningUsedResourceWeight) < epsilon) {
        return scoreLongRunningTask(longRunningMemUsedScore, memFreeScore, longRunningCpusUsedScore, cpusFreeScore);
      }
      freeResourceWeight = 1 - usedResourceWeight;
    }

    // usage reduces score
    return calculateScore(longRunningMemUsedScore, memFreeScore, longRunningCpusUsedScore, cpusFreeScore, freeResourceWeight, usedResourceWeight * -1);
  }

  private double calculateScore(double longRunningMemUsedScore, double memFreeScore, double longRunningCpusUsedScore, double cpusFreeScore, double freeResourceWeight, double usedResourceWeight) {
    double score = 0;

    score += (getNormalizedWeight(ResourceUsageType.CPU_USED) * usedResourceWeight) * longRunningCpusUsedScore;
    score += (getNormalizedWeight(ResourceUsageType.MEMORY_BYTES_USED) * usedResourceWeight) * longRunningMemUsedScore;

    score += (getNormalizedWeight(ResourceUsageType.CPU_FREE) * freeResourceWeight) * cpusFreeScore;
    score += (getNormalizedWeight(ResourceUsageType.MEMORY_BYTES_FREE) * freeResourceWeight) * memFreeScore;

    return score;
  }

  private SingularityMesosTaskHolder acceptTask(SingularityOfferHolder offerHolder, Map<String, Map<String, Integer>> tasksPerOfferPerRequest, SingularityTaskRequestHolder taskRequestHolder) {
    final SingularityTaskRequest taskRequest = taskRequestHolder.getTaskRequest();
    final SingularityMesosTaskHolder taskHolder = mesosTaskBuilder.buildTask(offerHolder, offerHolder.getCurrentResources(), taskRequest, taskRequestHolder.getTaskResources(), taskRequestHolder.getExecutorResources());

    final SingularityTask zkTask = taskSizeOptimizer.getSizeOptimizedTask(taskHolder);

    if (LOG.isTraceEnabled()) {
      LOG.trace("Accepted and built task {}", zkTask);
    }

    LOG.info("Launching task {} slot on slave {} ({})", taskHolder.getTask().getTaskId(), offerHolder.getSlaveId(), offerHolder.getHostname());

    taskManager.createTaskAndDeletePendingTask(zkTask);

    addRequestToMapByOfferHost(tasksPerOfferPerRequest, offerHolder.getHostname(), taskRequest.getRequest().getId());

    return taskHolder;
  }

  private void addRequestToMapByOfferHost(Map<String, Map<String, Integer>> tasksPerOfferHostPerRequest, String hostname, String requestId) {
    if (tasksPerOfferHostPerRequest.containsKey(hostname)) {
      if (tasksPerOfferHostPerRequest.get(hostname).containsKey(requestId)) {
        int count = tasksPerOfferHostPerRequest.get(hostname).get(requestId);
        tasksPerOfferHostPerRequest.get(hostname).put(requestId, count + 1);
      } else {
        tasksPerOfferHostPerRequest.get(hostname).put(requestId, 0);
      }
    } else {
      tasksPerOfferHostPerRequest.put(hostname, new HashMap<>());
      tasksPerOfferHostPerRequest.get(hostname).put(requestId, 1);
    }
  }

  private boolean tooManyTasksPerOfferHostForRequest(Map<String, Map<String, Integer>> tasksPerOfferHostPerRequest, String hostname, SingularityTaskRequest taskRequest) {
    if (!tasksPerOfferHostPerRequest.containsKey(hostname)) {
      return false;
    }
    if (!tasksPerOfferHostPerRequest.get(hostname).containsKey(taskRequest.getRequest().getId())) {
      return false;
    }

    int maxPerOfferPerRequest = taskRequest.getRequest().getMaxTasksPerOffer().or(configuration.getMaxTasksPerOfferPerRequest());
    return maxPerOfferPerRequest > 0 && tasksPerOfferHostPerRequest.get(hostname).get(taskRequest.getRequest().getId()) > maxPerOfferPerRequest;
  }

  private boolean isTooManyInstancesForRequest(SingularityTaskRequest taskRequest) {
    if (taskRequest.getRequest().getRequestType() == RequestType.ON_DEMAND) {
      int maxActiveOnDemandTasks = taskRequest.getRequest().getInstances().or(configuration.getMaxActiveOnDemandTasksPerRequest());
      if (maxActiveOnDemandTasks > 0) {
        int activeTasksForRequest = leaderCache.getActiveTaskIdsForRequest(taskRequest.getRequest().getId()).size();
        LOG.debug("Running {} instances for request {}. Max is {}", activeTasksForRequest, taskRequest.getRequest().getId(), maxActiveOnDemandTasks);
        if (activeTasksForRequest >= maxActiveOnDemandTasks) {
          return true;
        }
      }
    }

    return false;
  }
}
