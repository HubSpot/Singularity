package com.hubspot.singularity.mesos;

import java.util.ArrayList;
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
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.SlaveMatchState;
import com.hubspot.singularity.config.CustomExecutorConfiguration;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
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
  private final SingularityTaskSizeOptimizer taskSizeOptimizer;

  private final Provider<SingularitySchedulerStateCache> stateCacheProvider;
  private final SchedulerDriverSupplier schedulerDriverSupplier;

  @Inject
  public SingularityMesosOfferScheduler(MesosConfiguration mesosConfiguration, CustomExecutorConfiguration customExecutorConfiguration, TaskManager taskManager, SingularityMesosTaskPrioritizer taskPrioritizer,
      SingularityScheduler scheduler, SingularityConfiguration configuration, SingularityMesosTaskBuilder mesosTaskBuilder,
      SingularitySlaveAndRackManager slaveAndRackManager, SingularityTaskSizeOptimizer taskSizeOptimizer,
      Provider<SingularitySchedulerStateCache> stateCacheProvider, SchedulerDriverSupplier schedulerDriverSupplier) {
    this.defaultResources = new Resources(mesosConfiguration.getDefaultCpus(), mesosConfiguration.getDefaultMemory(), 0, mesosConfiguration.getDefaultDisk());
    this.defaultCustomExecutorResources = new Resources(customExecutorConfiguration.getNumCpus(), customExecutorConfiguration.getMemoryMb(), 0, customExecutorConfiguration.getDiskMb());
    this.taskManager = taskManager;
    this.scheduler = scheduler;
    this.configuration = configuration;
    this.mesosTaskBuilder = mesosTaskBuilder;
    this.slaveAndRackManager = slaveAndRackManager;
    this.taskSizeOptimizer = taskSizeOptimizer;
    this.stateCacheProvider = stateCacheProvider;
    this.schedulerDriverSupplier = schedulerDriverSupplier;
    this.taskPrioritizer = taskPrioritizer;
  }

  public List<SingularityOfferHolder> checkOffers(final List<Protos.Offer> offers, final Set<Protos.OfferID> acceptedOffers) {
    final SingularitySchedulerStateCache stateCache = stateCacheProvider.get();

    scheduler.checkForDecomissions(stateCache);
    scheduler.drainPendingQueue(stateCache);

    for (Protos.Offer offer : offers) {
      slaveAndRackManager.checkOffer(offer);
    }

    final List<SingularityTaskRequest> taskRequests = taskPrioritizer.getSortedDueTasks(scheduler.getDueTasks());

    for (SingularityTaskRequest taskRequest : taskRequests) {
      LOG.trace("Task {} is due", taskRequest.getPendingTask().getPendingTaskId());
    }

    taskPrioritizer.removeTasksAffectedByPriorityFreeze(taskRequests);

    final int numDueTasks = taskRequests.size();

    final List<SingularityOfferHolder> offerHolders = Lists.newArrayListWithCapacity(offers.size());
    final Map<String, Map<String, Integer>> tasksPerOfferPerRequest = new HashMap<>();
    for (Protos.Offer offer : offers) {
      offerHolders.add(new SingularityOfferHolder(offer, numDueTasks));
    }

    boolean addedTaskInLastLoop = true;

    int tasksScheduled = 0;

    while (!taskRequests.isEmpty() && addedTaskInLastLoop) {
      addedTaskInLastLoop = false;
      Collections.shuffle(offerHolders);

      for (SingularityOfferHolder offerHolder : offerHolders) {
        if (configuration.getMaxTasksPerOffer() > 0 && offerHolder.getAcceptedTasks().size() >= configuration.getMaxTasksPerOffer()) {
          LOG.trace("Offer {} is full ({}) - skipping", offerHolder.getOffer(), offerHolder.getAcceptedTasks().size());
          continue;
        }

        Optional<SingularityTask> accepted = match(taskRequests, stateCache, offerHolder, tasksPerOfferPerRequest);
        if (accepted.isPresent()) {
          tasksScheduled++;
          offerHolder.addMatchedTask(accepted.get());
          addedTaskInLastLoop = true;
          taskRequests.remove(accepted.get().getTaskRequest());
        }

        if (taskRequests.isEmpty()) {
          break;
        }
      }
    }

    LOG.info("{} tasks scheduled, {} tasks remaining after examining {} offers", tasksScheduled, numDueTasks - tasksScheduled, offers.size());

    return offerHolders;
  }

  private Optional<SingularityTask> match(Collection<SingularityTaskRequest> taskRequests, SingularitySchedulerStateCache stateCache, SingularityOfferHolder offerHolder,
      Map<String, Map<String, Integer>> tasksPerOfferPerRequest) {
    String offerId = offerHolder.getOffer().getId().getValue();
    for (SingularityTaskRequest taskRequest : taskRequests) {
      if (tooManyTasksPerOfferForRequest(tasksPerOfferPerRequest, offerId, taskRequest)) {
        LOG.debug("Skipping task request for request id {}, too many tasks already scheduled using offer {}", taskRequest.getRequest().getId(), offerId);
        continue;
      }

      final Resources taskResources = taskRequest.getPendingTask().getResources().or(taskRequest.getDeploy().getResources()).or(defaultResources);

      // only factor in executor resources if we're running a custom executor
      final Resources executorResources =
          taskRequest.getDeploy().getCustomExecutorCmd().isPresent() ? taskRequest.getDeploy().getCustomExecutorResources().or(defaultCustomExecutorResources) : Resources.EMPTY_RESOURCES;

      final Resources totalResources = Resources.add(taskResources, executorResources);

      final List<Long> requestedPorts = new ArrayList<>();

      if (taskRequest.getDeploy().getContainerInfo().isPresent() && taskRequest.getDeploy().getContainerInfo().get().getDocker().isPresent()) {
        requestedPorts.addAll(taskRequest.getDeploy().getContainerInfo().get().getDocker().get().getLiteralHostPorts());
      }

      Optional<String> requiredRole = taskRequest.getRequest().getRequiredRole();
      LOG.trace("Attempting to match task {} resources {} with required role '{}' ({} for task + {} for executor) with remaining offer resources {}", taskRequest.getPendingTask().getPendingTaskId(),
          totalResources, requiredRole.or("*"), taskResources, executorResources, offerHolder.getCurrentResources());

      final boolean matchesResources = MesosUtils.doesOfferMatchResources(requiredRole, totalResources, offerHolder.getCurrentResources(), requestedPorts);
      final SlaveMatchState slaveMatchState = slaveAndRackManager.doesOfferMatch(offerHolder.getOffer(), taskRequest, stateCache);

      if (matchesResources && slaveMatchState.isMatchAllowed()) {
        final SingularityTask task = mesosTaskBuilder.buildTask(offerHolder.getOffer(), offerHolder.getCurrentResources(), taskRequest, taskResources, executorResources);

        final SingularityTask zkTask = taskSizeOptimizer.getSizeOptimizedTask(task);

        LOG.trace("Accepted and built task {}", zkTask);

        LOG.info("Launching task {} slot on slave {} ({})", task.getTaskId(), offerHolder.getOffer().getSlaveId().getValue(), offerHolder.getOffer().getHostname());

        taskManager.createTaskAndDeletePendingTask(zkTask);

        stateCache.getActiveTaskIds().add(task.getTaskId());
        addRequestToMapByOfferId(tasksPerOfferPerRequest, offerId, taskRequest.getRequest().getId());
        stateCache.getScheduledTasks().remove(taskRequest.getPendingTask());

        return Optional.of(task);
      } else {
        String rolesInfo = MesosUtils.getRoles(offerHolder.getOffer()).toString();
        LOG.trace("Ignoring offer {} with roles {} on {} for task {}; matched resources: {}, slave match state: {}", offerHolder.getOffer().getId(), rolesInfo, offerHolder.getOffer().getHostname(),
            taskRequest.getPendingTask().getPendingTaskId(), matchesResources, slaveMatchState);
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
