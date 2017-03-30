package com.hubspot.singularity.mesos;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.apache.mesos.Protos;
import org.apache.mesos.Protos.Offer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.mesos.Resources;
import com.hubspot.mesos.json.MesosResourcesObject;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCurrentUsage;
import com.hubspot.singularity.SingularityTaskCurrentUsageWithId;
import com.hubspot.singularity.SingularityTaskIdHolder;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.SlaveMatchState;
import com.hubspot.singularity.config.CustomExecutorConfiguration;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DisasterManager;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.UsageManager;
import com.hubspot.singularity.scheduler.SingularityScheduler;
import com.hubspot.singularity.scheduler.SingularitySchedulerStateCache;

@Singleton
public class SingularityMesosOfferScheduler {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityMesosOfferScheduler.class);

  private static final String CPU_USED = "cpusUsed";
  private static final String CPU_TOTAL = "cpusTotal";
  private static final String MEMORY_USED = "memoryRssBytes";
  private static final String MEMORY_TOTAL = "memoryRssBytesTotal";

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
  private final SlaveManager slaveManager;

  private final Provider<SingularitySchedulerStateCache> stateCacheProvider;
  private final SchedulerDriverSupplier schedulerDriverSupplier;

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
                                        Provider<SingularitySchedulerStateCache> stateCacheProvider,
                                        SchedulerDriverSupplier schedulerDriverSupplier,
                                        DisasterManager disasterManager,
                                        UsageManager usageManager, SlaveManager slaveManager) {
    this.defaultResources = new Resources(mesosConfiguration.getDefaultCpus(), mesosConfiguration.getDefaultMemory(), 0, mesosConfiguration.getDefaultDisk());
    this.defaultCustomExecutorResources = new Resources(customExecutorConfiguration.getNumCpus(), customExecutorConfiguration.getMemoryMb(), 0, customExecutorConfiguration.getDiskMb());
    this.taskManager = taskManager;
    this.scheduler = scheduler;
    this.configuration = configuration;
    this.mesosTaskBuilder = mesosTaskBuilder;
    this.slaveAndRackManager = slaveAndRackManager;
    this.taskSizeOptimizer = taskSizeOptimizer;
    this.stateCacheProvider = stateCacheProvider;
    this.slaveAndRackHelper = slaveAndRackHelper;
    this.disasterManager = disasterManager;
    this.schedulerDriverSupplier = schedulerDriverSupplier;
    this.taskPrioritizer = taskPrioritizer;
    this.usageManager = usageManager;
    this.slaveManager = slaveManager;
  }

  public List<SingularityOfferHolder> checkOffers(final Collection<Protos.Offer> offers) {
    boolean useTaskCredits = disasterManager.isTaskCreditEnabled();
    int taskCredits = useTaskCredits ? disasterManager.getUpdatedCreditCount() : -1;
    final SingularitySchedulerStateCache stateCache = stateCacheProvider.get();

    scheduler.checkForDecomissions(stateCache);
    scheduler.drainPendingQueue(stateCache);

    for (Protos.Offer offer : offers) {
      slaveAndRackManager.checkOffer(offer);
    }

    final Map<String, SingularityTaskRequestHolder> pendingTaskIdToTaskRequest = getDueTaskRequestHolders();

    final int numDueTasks = pendingTaskIdToTaskRequest.size();

    final List<SingularityOfferHolder> offerHolders = Lists.newArrayListWithCapacity(offers.size());
    final Map<String, Map<String, Integer>> tasksPerOfferPerRequest = new HashMap<>();
    for (Protos.Offer offer : offers) {
      offerHolders.add(new SingularityOfferHolder(offer, numDueTasks, slaveAndRackHelper.getRackIdOrDefault(offer), slaveAndRackHelper.getTextAttributes(offer),
          slaveAndRackHelper.getReservedSlaveAttributes(offer)));
    }

    boolean addedTaskInLastLoop = true;

    int tasksScheduled = 0;

    while (!pendingTaskIdToTaskRequest.isEmpty() && addedTaskInLastLoop && canScheduleAdditionalTasks(taskCredits)) {
      addedTaskInLastLoop = false;

      for (SingularityTaskRequestHolder taskRequestHolder : pendingTaskIdToTaskRequest.values()) {

        Map<SingularityOfferHolder, Double> scorePerOffer = new HashMap<>();

        for (SingularityOfferHolder offerHolder : offerHolders) {

          if (configuration.getMaxTasksPerOffer() > 0 && offerHolder.getAcceptedTasks().size() >= configuration.getMaxTasksPerOffer()) {
            LOG.trace("Offer {} is full ({}) - skipping", offerHolder.getOffer(), offerHolder.getAcceptedTasks().size());
            continue;
          }

          double score = score(offerHolder, stateCache, tasksPerOfferPerRequest, taskRequestHolder, getUsagesPerRequestTypePerSlave());
          if (score > 0) {
            // todo: can short circuit here if score is high enough
            scorePerOffer.put(offerHolder, score);
          }
        }

        if (!scorePerOffer.isEmpty()) {
          SingularityOfferHolder bestOffer = Collections.max(scorePerOffer.entrySet(), Map.Entry.comparingByValue()).getKey();
          LOG.info("Best offer is {} with a score of {}/1", bestOffer, scorePerOffer.get(bestOffer));

          SingularityTask task = acceptTask(bestOffer, stateCache, tasksPerOfferPerRequest, taskRequestHolder);

          tasksScheduled++;
          if (useTaskCredits) {
            taskCredits--;
            LOG.debug("Remaining task credits: {}", taskCredits);
          }
          bestOffer.addMatchedTask(task);
          addedTaskInLastLoop = true;
          pendingTaskIdToTaskRequest.remove(task.getTaskRequest().getPendingTask().getPendingTaskId().getId());
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

  public boolean isConnected() {
    return schedulerDriverSupplier.get().isPresent();
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

  private Map<String, Map<RequestType, Map<String, Integer>>> getUsagesPerRequestTypePerSlave() {
    List<String> slavesWithUsage = usageManager.getSlavesWithUsage();

    Map<String, Map<RequestType, List<SingularityTask>>> tasksPerRequestTypePerSlave = taskManager.getActiveTasks().stream()
        .filter(t -> slavesWithUsage.contains(t.getOffer().getSlaveId().getValue()))
        .collect(Collectors.groupingBy(t -> t.getOffer().getSlaveId().getValue(),
            Collectors.groupingBy(t -> t.getTaskRequest().getRequest().getRequestType())));

    Map<String, Map<RequestType, Map<String, Integer>>> usagesPerRequestTypePerSlave = new HashMap<>();

    for (String slaveId : tasksPerRequestTypePerSlave.keySet()) {
      final Optional<SingularitySlave> slave = slaveManager.getObject(slaveId);
      if (!slave.isPresent() || !slave.get().getResources().isPresent() ||
          !slave.get().getResources().get().getMemoryMegaBytes().isPresent() ||
          !slave.get().getResources().get().getNumCpus().isPresent()) {
        LOG.debug("Could not find slave or resources for slave {}, skipping", slaveId);
        continue;
      }

      final MesosResourcesObject resources = slave.get().getResources().get();

      int totalCpu = resources.getMemoryMegaBytes().get();
      int totalMem = resources.getNumCpus().get();
      Map<RequestType, Map<String, Integer>> usagesPerRequestType = new HashMap<>();

      for (RequestType type : RequestType.values()) {
        List<SingularityTaskCurrentUsageWithId> usages = usageManager.getTaskCurrentUsages(tasksPerRequestTypePerSlave.get(slaveId).get(type).stream().map(SingularityTaskIdHolder::getTaskId).collect(Collectors.toList()));
        long memUsed = usages.stream().mapToLong(SingularityTaskCurrentUsage::getMemoryRssBytes).sum();
        double cpuUsed = usages.stream().mapToDouble(SingularityTaskCurrentUsage::getCpusUsed).sum();

        usagesPerRequestType.put(type, ImmutableMap.of(MEMORY_USED, ((int) memUsed)));
        usagesPerRequestType.put(type, ImmutableMap.of(MEMORY_TOTAL, totalMem));
        usagesPerRequestType.put(type, ImmutableMap.of(CPU_USED, ((int) cpuUsed)));
        usagesPerRequestType.put(type, ImmutableMap.of(CPU_TOTAL, totalCpu));
      }

      usagesPerRequestTypePerSlave.put(slaveId, usagesPerRequestType);
    }

    return usagesPerRequestTypePerSlave;
  }

  private boolean canScheduleAdditionalTasks(int taskCredits) {
    return taskCredits == -1 || taskCredits > 0;
  }

  private double score(SingularityOfferHolder offerHolder, SingularitySchedulerStateCache stateCache, Map<String, Map<String, Integer>> tasksPerOfferPerRequest,
                    SingularityTaskRequestHolder taskRequestHolder, Map<String, Map<RequestType, Map<String, Integer>>> usagesPerRequestTypePerSlave) {

    final Offer offer = offerHolder.getOffer();
    final String offerId = offer.getId().getValue();
    final SingularityTaskRequest taskRequest = taskRequestHolder.getTaskRequest();
    final SingularityPendingTaskId pendingTaskId = taskRequest.getPendingTask().getPendingTaskId();

    if (offerHolder.hasRejectedPendingTaskAlready(pendingTaskId)) {
      return 0;
    }

    if (tooManyTasksPerOfferForRequest(tasksPerOfferPerRequest, offerId, taskRequestHolder.getTaskRequest())) {
      LOG.debug("Skipping task request for request id {}, too many tasks already scheduled using offer {}", taskRequest.getRequest().getId(), offerId);
      return 0;
    }

    if (isTooManyInstancesForRequest(taskRequest, stateCache)) {
      LOG.debug("Skipping pending task {}, too many instances already running", pendingTaskId);
      return 0;
    }

    if (LOG.isTraceEnabled()) {
      LOG.trace("Attempting to match task {} resources {} with required role '{}' ({} for task + {} for executor) with remaining offer resources {}",
          pendingTaskId, taskRequestHolder.getTotalResources(), taskRequest.getRequest().getRequiredRole().or("*"),
          taskRequestHolder.getTaskResources(), taskRequestHolder.getExecutorResources(), offerHolder.getCurrentResources());
    }

    final boolean matchesResources = MesosUtils.doesOfferMatchResources(taskRequest.getRequest().getRequiredRole(),
        taskRequestHolder.getTotalResources(), offerHolder.getCurrentResources(), taskRequestHolder.getRequestedPorts());
    final SlaveMatchState slaveMatchState = slaveAndRackManager.doesOfferMatch(offerHolder, taskRequest, stateCache);

    if (matchesResources && slaveMatchState.isMatchAllowed()) {
      return score(offer, taskRequest, usagesPerRequestTypePerSlave);
    } else {
      offerHolder.addRejectedTask(pendingTaskId);

      if (LOG.isTraceEnabled()) {
        LOG.trace("Ignoring offer {} with roles {} on {} for task {}; matched resources: {}, slave match state: {}", offerId,
            MesosUtils.getRoles(offer), offer.getHostname(), pendingTaskId, matchesResources, slaveMatchState);
      }
    }

    return 0;
  }

  private double score(Offer offer, SingularityTaskRequest taskRequest, Map<String, Map<RequestType, Map<String, Integer>>> usagesPerRequestTypePerSlave) {
    double requestTypeCpuWeight = 0.20;
    double requestTypeMemWeight = 0.30;
    double freeCpuWeight = 0.20;
    double freeMemWeight = 0.30;
    double score = 0;
    double defaultScoreForMissingUsage = 0.10;

    String slaveId = offer.getSlaveId().getValue();
    if (!usagesPerRequestTypePerSlave.containsKey(slaveId)) {
      LOG.info("Offer {} has no usage data. Will default to {}", offer.getId(), defaultScoreForMissingUsage);
      return defaultScoreForMissingUsage;
    }

    RequestType requestType = taskRequest.getRequest().getRequestType();
    Map<String, Integer> usagePerResource = usagesPerRequestTypePerSlave.get(slaveId).get(requestType);

    score += requestTypeCpuWeight * (1 - (usagePerResource.get(CPU_USED) / usagePerResource.get(CPU_TOTAL)));
    score += requestTypeMemWeight * (1 - (usagePerResource.get(MEMORY_USED) / usagePerResource.get(MEMORY_TOTAL)));

    score += freeCpuWeight * (MesosUtils.getNumCpus(offer) / usagePerResource.get(CPU_TOTAL));
    score += freeMemWeight * (MesosUtils.getMemory(offer) / usagePerResource.get(MEMORY_TOTAL));

    return score;
  }

  private SingularityTask acceptTask(SingularityOfferHolder offerHolder, SingularitySchedulerStateCache stateCache, Map<String, Map<String, Integer>> tasksPerOfferPerRequest, SingularityTaskRequestHolder taskRequestHolder) {
    final SingularityTaskRequest taskRequest = taskRequestHolder.getTaskRequest();
    final SingularityTask task = mesosTaskBuilder.buildTask(offerHolder.getOffer(), offerHolder.getCurrentResources(), taskRequest, taskRequestHolder.getTaskResources(), taskRequestHolder.getExecutorResources());

    final SingularityTask zkTask = taskSizeOptimizer.getSizeOptimizedTask(task);

    if (LOG.isTraceEnabled()) {
      LOG.trace("Accepted and built task {}", zkTask);
    }

    LOG.info("Launching task {} slot on slave {} ({})", task.getTaskId(), offerHolder.getOffer().getSlaveId().getValue(), offerHolder.getOffer().getHostname());

    taskManager.createTaskAndDeletePendingTask(zkTask);

    stateCache.getActiveTaskIds().add(task.getTaskId());
    stateCache.getActiveTaskIdsForRequest(task.getTaskRequest().getRequest().getId()).add(task.getTaskId());
    addRequestToMapByOfferId(tasksPerOfferPerRequest, offerHolder.getOffer().getId().getValue(), taskRequest.getRequest().getId());
    stateCache.getScheduledTasks().remove(taskRequest.getPendingTask());

    return task;
  }

  private void addRequestToMapByOfferId(Map<String, Map<String, Integer>> tasksPerOfferPerRequest, String offerId, String requestId) {
    if (tasksPerOfferPerRequest.containsKey(offerId)) {
      if (tasksPerOfferPerRequest.get(offerId).containsKey(requestId)) {
        int count = tasksPerOfferPerRequest.get(offerId).get(requestId);
        tasksPerOfferPerRequest.get(offerId).put(requestId, count + 1);
      } else {
        tasksPerOfferPerRequest.get(offerId).put(requestId, 0);
      }
    } else {
      tasksPerOfferPerRequest.put(offerId, new HashMap<String, Integer>());
      tasksPerOfferPerRequest.get(offerId).put(requestId, 1);
    }
  }

  private boolean tooManyTasksPerOfferForRequest(Map<String, Map<String, Integer>> tasksPerOfferPerRequest, String offerId, SingularityTaskRequest taskRequest) {
    if (!tasksPerOfferPerRequest.containsKey(offerId)) {
      return false;
    }
    if (!tasksPerOfferPerRequest.get(offerId).containsKey(taskRequest.getRequest().getId())) {
      return false;
    }

    int maxPerOfferPerRequest = taskRequest.getRequest().getMaxTasksPerOffer().or(configuration.getMaxTasksPerOfferPerRequest());
    if (!(maxPerOfferPerRequest > 0)) {
      return false;
    }
    return tasksPerOfferPerRequest.get(offerId).get(taskRequest.getRequest().getId()) > maxPerOfferPerRequest;
  }

  private boolean isTooManyInstancesForRequest(SingularityTaskRequest taskRequest, SingularitySchedulerStateCache stateCache) {
    if (taskRequest.getRequest().getRequestType() == RequestType.ON_DEMAND) {
      int maxActiveOnDemandTasks = taskRequest.getRequest().getInstances().or(configuration.getMaxActiveOnDemandTasksPerRequest());
      if (maxActiveOnDemandTasks > 0) {
        int activeTasksForRequest = stateCache.getActiveTaskIdsForRequest(taskRequest.getRequest().getId()).size();
        LOG.debug("Running {} instances for request {}. Max is {}", activeTasksForRequest, taskRequest.getRequest().getId(), maxActiveOnDemandTasks);
        if (activeTasksForRequest >= maxActiveOnDemandTasks) {
          return true;
        }
      }
    }

    return false;
  }
}
