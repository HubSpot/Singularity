package com.hubspot.singularity.mesos;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;

import org.apache.mesos.Protos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.mesos.Resources;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.SlaveMatchState;
import com.hubspot.singularity.config.CustomExecutorConfiguration;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DisasterManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.scheduler.SingularityScheduler;
import com.hubspot.singularity.scheduler.SingularitySchedulerStateCache;

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

  private final Provider<SingularitySchedulerStateCache> stateCacheProvider;
  private final SchedulerDriverSupplier schedulerDriverSupplier;

  @Inject
  public SingularityMesosOfferScheduler(MesosConfiguration mesosConfiguration, CustomExecutorConfiguration customExecutorConfiguration, TaskManager taskManager, SingularityMesosTaskPrioritizer taskPrioritizer,
      SingularityScheduler scheduler, SingularityConfiguration configuration, SingularityMesosTaskBuilder mesosTaskBuilder,
      SingularitySlaveAndRackManager slaveAndRackManager, SingularityTaskSizeOptimizer taskSizeOptimizer, SingularitySlaveAndRackHelper slaveAndRackHelper,
      Provider<SingularitySchedulerStateCache> stateCacheProvider, SchedulerDriverSupplier schedulerDriverSupplier, DisasterManager disasterManager) {
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

  public List<SingularityOfferHolder> checkOffers(final Collection<Protos.Offer> offers, final Set<Protos.OfferID> acceptedOffers) {
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
      Collections.shuffle(offerHolders);

      for (SingularityOfferHolder offerHolder : offerHolders) {
        if (configuration.getMaxTasksPerOffer() > 0 && offerHolder.getAcceptedTasks().size() >= configuration.getMaxTasksPerOffer()) {
          LOG.trace("Offer {} is full ({}) - skipping", offerHolder.getOffer(), offerHolder.getAcceptedTasks().size());
          continue;
        }

        Optional<SingularityTask> accepted = match(pendingTaskIdToTaskRequest.values(), stateCache, offerHolder, tasksPerOfferPerRequest);
        if (accepted.isPresent()) {
          tasksScheduled++;
          if (useTaskCredits) {
            taskCredits--;
            LOG.debug("Remaining task credits: {}", taskCredits);
          }
          offerHolder.addMatchedTask(accepted.get());
          addedTaskInLastLoop = true;
          pendingTaskIdToTaskRequest.remove(accepted.get().getTaskRequest().getPendingTask().getPendingTaskId().getId());
          if (useTaskCredits && taskCredits == 0) {
            LOG.info("Used all available task credits, not scheduling any more tasks");
            break;
          }
        }

        if (pendingTaskIdToTaskRequest.isEmpty()) {
          break;
        }
      }
    }

    if (useTaskCredits) {
      disasterManager.saveTaskCreditCount(taskCredits);
    }

    LOG.info("{} tasks scheduled, {} tasks remaining after examining {} offers", tasksScheduled, numDueTasks - tasksScheduled, offers.size());

    return offerHolders;
  }

  private boolean canScheduleAdditionalTasks(int taskCredits) {
    return taskCredits == -1 || taskCredits > 0;
  }

  private Optional<SingularityTask> match(Collection<SingularityTaskRequestHolder> taskRequests, SingularitySchedulerStateCache stateCache, SingularityOfferHolder offerHolder,
      Map<String, Map<String, Integer>> tasksPerOfferPerRequest) {
    final String offerId = offerHolder.getOffer().getId().getValue();
    for (SingularityTaskRequestHolder taskRequestHolder : taskRequests) {
      final SingularityTaskRequest taskRequest = taskRequestHolder.getTaskRequest();

      if (offerHolder.hasRejectedPendingTaskAlready(taskRequest.getPendingTask().getPendingTaskId())) {
        continue;
      }

      if (tooManyTasksPerOfferForRequest(tasksPerOfferPerRequest, offerId, taskRequestHolder.getTaskRequest())) {
        LOG.debug("Skipping task request for request id {}, too many tasks already scheduled using offer {}", taskRequest.getRequest().getId(), offerId);
        continue;
      }

      if (taskRequest.getRequest().getRequestType() == RequestType.ON_DEMAND) {
        int maxActiveOnDemandTasks = taskRequest.getRequest().getInstances().or(configuration.getMaxActiveOnDemandTasksPerRequest());
        if (maxActiveOnDemandTasks > 0) {
          int activeTasksForRequest = stateCache.getActiveTaskIdsForRequest(taskRequest.getRequest().getId()).size();
          if (activeTasksForRequest >= maxActiveOnDemandTasks) {
            LOG.debug("Skipping pending task {}, already running {} instances for request {} (max is {})", taskRequest.getPendingTask().getPendingTaskId(), activeTasksForRequest);
            continue;
          }
        }
      }

      if (LOG.isTraceEnabled()) {
        LOG.trace("Attempting to match task {} resources {} with required role '{}' ({} for task + {} for executor) with remaining offer resources {}", taskRequest.getPendingTask().getPendingTaskId(),
          taskRequestHolder.getTotalResources(), taskRequest.getRequest().getRequiredRole().or("*"), taskRequestHolder.getTaskResources(), taskRequestHolder.getExecutorResources(),
          offerHolder.getCurrentResources());
      }

      final boolean matchesResources = MesosUtils.doesOfferMatchResources(taskRequest.getRequest().getRequiredRole(), taskRequestHolder.getTotalResources(), offerHolder.getCurrentResources(),
          taskRequestHolder.getRequestedPorts());
      final SlaveMatchState slaveMatchState = slaveAndRackManager.doesOfferMatch(offerHolder, taskRequest, stateCache);

      if (matchesResources && slaveMatchState.isMatchAllowed()) {
        final SingularityTask task = mesosTaskBuilder.buildTask(offerHolder.getOffer(), offerHolder.getCurrentResources(), taskRequest, taskRequestHolder.getTaskResources(), taskRequestHolder.getExecutorResources());

        final SingularityTask zkTask = taskSizeOptimizer.getSizeOptimizedTask(task);

        if (LOG.isTraceEnabled()) {
          LOG.trace("Accepted and built task {}", zkTask);
        }

        LOG.info("Launching task {} slot on slave {} ({})", task.getTaskId(), offerHolder.getOffer().getSlaveId().getValue(), offerHolder.getOffer().getHostname());

        taskManager.createTaskAndDeletePendingTask(zkTask);

        stateCache.getActiveTaskIds().add(task.getTaskId());
        stateCache.getActiveTaskIdsForRequest(task.getTaskRequest().getRequest().getId()).add(task.getTaskId());
        addRequestToMapByOfferId(tasksPerOfferPerRequest, offerId, taskRequest.getRequest().getId());
        stateCache.getScheduledTasks().remove(taskRequest.getPendingTask());

        return Optional.of(task);
      } else {
        offerHolder.addRejectedTask(taskRequest.getPendingTask().getPendingTaskId());

        if (LOG.isTraceEnabled()) {
          LOG.trace("Ignoring offer {} with roles {} on {} for task {}; matched resources: {}, slave match state: {}", offerHolder.getOffer().getId().getValue(),
              MesosUtils.getRoles(offerHolder.getOffer()), offerHolder.getOffer().getHostname(), taskRequest.getPendingTask().getPendingTaskId(), matchesResources, slaveMatchState);
        }
      }
    }

    return Optional.absent();
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

  public boolean isConnected() {
    return schedulerDriverSupplier.get().isPresent();
  }

}
