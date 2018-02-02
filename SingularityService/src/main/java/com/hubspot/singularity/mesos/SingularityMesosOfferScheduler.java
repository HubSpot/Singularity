package com.hubspot.singularity.mesos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.apache.mesos.v1.Protos.Offer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.mesos.Resources;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityDeployStatistics;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularitySlaveUsage;
import com.hubspot.singularity.SingularitySlaveUsage.ResourceUsageType;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.SlaveMatchState;
import com.hubspot.singularity.config.CustomExecutorConfiguration;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.UsageManager;
import com.hubspot.singularity.helpers.MesosUtils;
import com.hubspot.singularity.helpers.SingularityMesosTaskHolder;
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
  private final UsageManager usageManager;
  private final DeployManager deployManager;
  private final SingularitySchedulerLock lock;
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
                                        UsageManager usageManager,
                                        DeployManager deployManager,
                                        SingularitySchedulerLock lock) {
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
    this.taskPrioritizer = taskPrioritizer;
    this.usageManager = usageManager;
    this.deployManager = deployManager;
    this.lock = lock;
  }

  public List<SingularityOfferHolder> checkOffers(final Collection<Offer> offers) {
    for (SingularityPendingTaskId taskId : taskManager.getPendingTasksMarkedForDeletion()) {
      lock.runWithRequestLock(() -> taskManager.deletePendingTask(taskId), taskId.getRequestId(), "pendingTaskDeletes");
    }

    scheduler.checkForDecomissions();
    scheduler.drainPendingQueue();

    if (offers.isEmpty()) {
      LOG.debug("No offers to check");
      return Collections.emptyList();
    }

    final List<SingularityTaskRequest> sortedTaskRequests = getSortedDueTaskRequests();
    final List<SingularityTaskRequestHolder> sortedTaskRequestHolders = new ArrayList<>();
    final int numDueTasks = sortedTaskRequestHolders.size();

    final List<SingularityOfferHolder> offerHolders = offers.stream()
        .collect(Collectors.groupingBy((o) -> o.getAgentId().getValue()))
        .entrySet().parallelStream()
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

    if (sortedTaskRequests.isEmpty()) {
      return offerHolders;
    }

    Double smallestMemAsk = null;
    Double smallestCpuAsk = null;
    Double smallestDiskAsk = null;

    for (SingularityTaskRequest taskRequest : getSortedDueTaskRequests()) {
      SingularityTaskRequestHolder taskRequestHolder = new SingularityTaskRequestHolder(taskRequest, defaultResources, defaultCustomExecutorResources);
      sortedTaskRequestHolders.add(taskRequestHolder);
      if (smallestDiskAsk == null || taskRequestHolder.getTotalResources().getDiskMb() < smallestDiskAsk) {
        smallestDiskAsk = taskRequestHolder.getTotalResources().getDiskMb();
      }
      if (smallestCpuAsk == null || taskRequestHolder.getTotalResources().getCpus() < smallestCpuAsk) {
        smallestCpuAsk = taskRequestHolder.getTotalResources().getCpus();
      }
      if (smallestMemAsk == null || taskRequestHolder.getTotalResources().getMemoryMb() < smallestMemAsk) {
        smallestMemAsk = taskRequestHolder.getTotalResources().getMemoryMb();
      }
    }

    final Resources minResources = new Resources(smallestCpuAsk, smallestMemAsk, 0, smallestDiskAsk);

    final Map<String, Map<String, Integer>> tasksPerOfferPerRequest = new HashMap<>();
    final AtomicInteger tasksScheduled = new AtomicInteger(0);
    final Map<String, SingularitySlaveUsage> currentSlaveUsagesBySlaveId = usageManager.getCurrentSlaveUsages(offerHolders.stream().map(SingularityOfferHolder::getSlaveId).collect(Collectors.toList()));
    final List<SingularityOfferHolder> fullOffers = new ArrayList<>();

    for (SingularityTaskRequestHolder taskRequestHolder : sortedTaskRequestHolders) {
      lock.runWithRequestLock(() -> {
        Map<SingularityOfferHolder, Double> scorePerOffer = offerHolders
            .parallelStream()
            .filter((offerHolder) ->
                !(configuration.getMaxTasksPerOffer() > 0 && offerHolder.getAcceptedTasks().size() >= configuration.getMaxTasksPerOffer())
                    && !fullOffers.contains(offerHolder))
            .collect(Collectors.toMap(
                Function.identity(),
                (offerHolder) -> calculateScore(offerHolder, currentSlaveUsagesBySlaveId, tasksPerOfferPerRequest, taskRequestHolder)
            ));
        java.util.Optional<SingularityOfferHolder> bestOffer = scorePerOffer.keySet()
            .stream()
            .filter((offerHolder) -> scorePerOffer.get(offerHolder) > 0)
            .max(Comparator.comparingDouble(scorePerOffer::get));

        if (bestOffer.isPresent() && scorePerOffer.get(bestOffer.get()) > 0) {
          LOG.info("Best offer {}/1 is on {}", scorePerOffer.get(bestOffer.get()), bestOffer.get().getSanitizedHost());
          SingularityMesosTaskHolder taskHolder = acceptTask(bestOffer.get(), tasksPerOfferPerRequest, taskRequestHolder);
          tasksScheduled.getAndIncrement();
          bestOffer.get().addMatchedTask(taskHolder);

          // If this offer doesn't have any of the minimum requested resources left, skip it for future iterations
          if (!MesosUtils.doesOfferMatchResources(Optional.absent(), minResources, bestOffer.get().getCurrentResources(), Collections.emptyList())) {
            fullOffers.add(bestOffer.get());
          }
        }
      }, taskRequestHolder.getTaskRequest().getRequest().getId(), "checkOffers");
    }

    LOG.info("{} tasks scheduled, {} tasks remaining after examining {} offers", tasksScheduled, numDueTasks - tasksScheduled.get(), offers.size());

    return offerHolders;
  }

  private double calculateScore(SingularityOfferHolder offerHolder, Map<String, SingularitySlaveUsage> currentSlaveUsagesBySlaveId, Map<String, Map<String, Integer>> tasksPerOfferPerRequest, SingularityTaskRequestHolder taskRequestHolder) {
    Optional<SingularitySlaveUsage> maybeSlaveUsage = Optional.fromNullable(currentSlaveUsagesBySlaveId.get(offerHolder.getSlaveId()));
    double score = score(offerHolder, tasksPerOfferPerRequest, taskRequestHolder, maybeSlaveUsage);
    LOG.trace("Scored {} | Task {} | Offer - mem {} - cpu {} | Slave {} | maybeSlaveUsage - {}", score, taskRequestHolder.getTaskRequest().getPendingTask().getPendingTaskId().getId(),
        MesosUtils.getMemory(offerHolder.getCurrentResources(), Optional.absent()), MesosUtils.getNumCpus(offerHolder.getCurrentResources(), Optional.absent()), offerHolder.getHostname(), maybeSlaveUsage);
    return score;
  }

  private double getNormalizedWeight(ResourceUsageType type) {
    double freeCpuWeight = configuration.getFreeCpuWeightForOffer();
    double freeMemWeight = configuration.getFreeMemWeightForOffer();
    double freeDiskWeight = configuration.getFreeDiskWeightForOffer();
    double usedCpuWeight = configuration.getLongRunningUsedCpuWeightForOffer();
    double usedMemWeight = configuration.getLongRunningUsedMemWeightForOffer();
    double usedDiskWeight = configuration.getLongRunningUsedDiskWeightForOffer();

    switch (type) {
      case CPU_FREE:
        return freeCpuWeight + freeMemWeight + freeDiskWeight != 1 ? freeCpuWeight / (freeCpuWeight + freeMemWeight + freeDiskWeight) : freeCpuWeight;
      case MEMORY_BYTES_FREE:
        return freeCpuWeight + freeMemWeight + freeDiskWeight != 1 ? freeMemWeight / (freeCpuWeight + freeMemWeight + freeDiskWeight) : freeMemWeight;
      case DISK_BYTES_FREE:
        return freeCpuWeight + freeMemWeight + freeDiskWeight != 1 ? freeDiskWeight / (freeCpuWeight + freeMemWeight + freeDiskWeight) : freeDiskWeight;
      case CPU_USED:
        return usedCpuWeight + usedMemWeight + usedDiskWeight != 1 ? usedCpuWeight / (usedCpuWeight + usedMemWeight + usedDiskWeight) : usedCpuWeight;
      case MEMORY_BYTES_USED:
        return usedCpuWeight + usedMemWeight + usedDiskWeight != 1 ? usedMemWeight / (usedCpuWeight + usedMemWeight + usedDiskWeight) : usedMemWeight;
      case DISK_BYTES_USED:
        return usedCpuWeight + usedMemWeight + usedDiskWeight != 1 ? usedDiskWeight / (usedCpuWeight + usedMemWeight + usedDiskWeight) : usedDiskWeight;
      default:
        LOG.error("Invalid ResourceUsageType {}", type);
        return 0;
    }
  }

  private List<SingularityTaskRequest> getSortedDueTaskRequests() {
    final List<SingularityTaskRequest> taskRequests = taskPrioritizer.getSortedDueTasks(scheduler.getDueTasks());

    taskRequests.forEach((taskRequest) -> LOG.trace("Task {} is due", taskRequest.getPendingTask().getPendingTaskId()));

    taskPrioritizer.removeTasksAffectedByPriorityFreeze(taskRequests);

    return taskRequests;
  }

  private double score(SingularityOfferHolder offerHolder, Map<String, Map<String, Integer>> tasksPerOfferHostPerRequest,
                       SingularityTaskRequestHolder taskRequestHolder, Optional<SingularitySlaveUsage> maybeSlaveUsage) {

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
  double score(String hostname, SingularityTaskRequest taskRequest, Optional<SingularitySlaveUsage> maybeSlaveUsage) {
    if (isMissingUsageData(maybeSlaveUsage)) {
      LOG.info("Slave {} has missing usage data ({}). Will default to {}", hostname, maybeSlaveUsage, configuration.getDefaultOfferScoreForMissingUsage());
      return configuration.getDefaultOfferScoreForMissingUsage();
    }

    SingularitySlaveUsage slaveUsage = maybeSlaveUsage.get();
    Map<ResourceUsageType, Number> longRunningTasksUsage = slaveUsage.getLongRunningTasksUsage();

    double longRunningCpusUsedScore = longRunningTasksUsage.get(ResourceUsageType.CPU_USED).doubleValue() / slaveUsage.getCpusTotal().get();
    double longRunningMemUsedScore = ((double) longRunningTasksUsage.get(ResourceUsageType.MEMORY_BYTES_USED).longValue() / slaveUsage.getMemoryBytesTotal().get());
    double longRunningDiskUsedScore = ((double) longRunningTasksUsage.get(ResourceUsageType.DISK_BYTES_USED).longValue() / slaveUsage.getDiskBytesTotal().get());

    double cpusFreeScore = 1 - (slaveUsage.getCpusReserved() / slaveUsage.getCpusTotal().get());
    double memFreeScore = 1 - ((double) slaveUsage.getMemoryMbReserved() / slaveUsage.getMemoryMbTotal().get());
    double diskFreeScore = 1 - ((double) slaveUsage.getDiskMbReserved() / slaveUsage.getDiskMbTotal().get());

    return isLongRunning(taskRequest) ? scoreLongRunningTask(longRunningMemUsedScore, memFreeScore, longRunningCpusUsedScore, cpusFreeScore, longRunningDiskUsedScore, diskFreeScore)
        : scoreNonLongRunningTask(taskRequest, longRunningMemUsedScore, memFreeScore, longRunningCpusUsedScore, cpusFreeScore, longRunningDiskUsedScore, diskFreeScore);
  }

  private boolean isMissingUsageData(Optional<SingularitySlaveUsage> maybeSlaveUsage) {
    return !maybeSlaveUsage.isPresent() ||
        !maybeSlaveUsage.get().getCpusTotal().isPresent() || !maybeSlaveUsage.get().getMemoryMbTotal().isPresent() || !maybeSlaveUsage.get().getDiskMbTotal().isPresent() ||
        maybeSlaveUsage.get().getLongRunningTasksUsage() == null ||
        !maybeSlaveUsage.get().getLongRunningTasksUsage().containsKey(ResourceUsageType.CPU_USED) ||
        !maybeSlaveUsage.get().getLongRunningTasksUsage().containsKey(ResourceUsageType.MEMORY_BYTES_USED) ||
        !maybeSlaveUsage.get().getLongRunningTasksUsage().containsKey(ResourceUsageType.DISK_BYTES_USED);
  }

  private boolean isLongRunning(SingularityTaskRequest taskRequest) {
    return taskRequest.getRequest().getRequestType().isLongRunning();
  }

  private double scoreLongRunningTask(double longRunningMemUsedScore, double memFreeScore, double longRunningCpusUsedScore, double cpusFreeScore, double longRunningDiskUsedScore, double diskFreeScore) {
    // unused, reserved resources improve score
    return calculateScore(1 - longRunningMemUsedScore, memFreeScore, 1 - longRunningCpusUsedScore, cpusFreeScore, 1 - longRunningDiskUsedScore, diskFreeScore, 0.50, 0.50);
  }

  private double scoreNonLongRunningTask(SingularityTaskRequest taskRequest, double longRunningMemUsedScore, double memFreeScore, double longRunningCpusUsedScore, double cpusFreeScore, double longRunningDiskUsedScore, double diskFreeScore) {
    Optional<SingularityDeployStatistics> statistics = deployManager.getDeployStatistics(taskRequest.getRequest().getId(), taskRequest.getDeploy().getId());
    final double epsilon = 0.0001;

    double freeResourceWeight = 0.75;
    double usedResourceWeight = 0.25;

    if (statistics.isPresent() && statistics.get().getAverageRuntimeMillis().isPresent()) {
      final double maxNonLongRunningUsedResourceWeight = configuration.getMaxNonLongRunningUsedResourceWeight();
      usedResourceWeight = Math.min((double) TimeUnit.MILLISECONDS.toSeconds(statistics.get().getAverageRuntimeMillis().get()) / configuration.getConsiderNonLongRunningTaskLongRunningAfterRunningForSeconds(), 1) * maxNonLongRunningUsedResourceWeight;

      if (Math.abs(usedResourceWeight - maxNonLongRunningUsedResourceWeight) < epsilon) {
        return scoreLongRunningTask(longRunningMemUsedScore, memFreeScore, longRunningCpusUsedScore, cpusFreeScore, longRunningDiskUsedScore, diskFreeScore);
      }
      freeResourceWeight = 1 - usedResourceWeight;
    }

    // usage reduces score
    return calculateScore(longRunningMemUsedScore, memFreeScore, longRunningCpusUsedScore, cpusFreeScore, longRunningDiskUsedScore, diskFreeScore, freeResourceWeight, usedResourceWeight * -1);
  }

  private double calculateScore(double longRunningMemUsedScore, double memFreeScore, double longRunningCpusUsedScore, double cpusFreeScore, double longRunningDiskUsedScore, double diskFreeScore, double freeResourceWeight, double usedResourceWeight) {
    double score = 0;

    score += (getNormalizedWeight(ResourceUsageType.CPU_USED) * usedResourceWeight) * longRunningCpusUsedScore;
    score += (getNormalizedWeight(ResourceUsageType.MEMORY_BYTES_USED) * usedResourceWeight) * longRunningMemUsedScore;
    score += (getNormalizedWeight(ResourceUsageType.DISK_BYTES_USED) * usedResourceWeight) * longRunningDiskUsedScore;

    score += (getNormalizedWeight(ResourceUsageType.CPU_FREE) * freeResourceWeight) * cpusFreeScore;
    score += (getNormalizedWeight(ResourceUsageType.MEMORY_BYTES_FREE) * freeResourceWeight) * memFreeScore;
    score += (getNormalizedWeight(ResourceUsageType.DISK_BYTES_FREE) * freeResourceWeight) * diskFreeScore;

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
