package com.hubspot.singularity.mesos;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.mesos.Protos;
import org.apache.mesos.Protos.TaskState;
import org.apache.mesos.Scheduler;
import org.apache.mesos.SchedulerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.mesos.Resources;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.mesos.SingularitySlaveAndRackManager.SlaveMatchState;
import com.hubspot.singularity.scheduler.SingularityHealthchecker;
import com.hubspot.singularity.scheduler.SingularityNewTaskChecker;
import com.hubspot.singularity.scheduler.SingularityScheduler;
import com.hubspot.singularity.scheduler.SingularitySchedulerStateCache;

public class SingularityMesosScheduler implements Scheduler {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityMesosScheduler.class);

  private final Resources defaultResources;
  private final TaskManager taskManager;
  private final DeployManager deployManager;
  private final SingularityScheduler scheduler;
  private final SingularityMesosTaskBuilder mesosTaskBuilder;
  private final SingularityHealthchecker healthchecker;
  private final SingularityNewTaskChecker newTaskChecker;
  private final SingularitySlaveAndRackManager slaveAndRackManager;
  private final SingularityLogSupport logSupport;

  private final Provider<SingularitySchedulerStateCache> stateCacheProvider;

  @Inject
  public SingularityMesosScheduler(MesosConfiguration mesosConfiguration, TaskManager taskManager, SingularityScheduler scheduler, SingularitySlaveAndRackManager slaveAndRackManager, SingularityNewTaskChecker newTaskChecker,
      SingularityMesosTaskBuilder mesosTaskBuilder, SingularityLogSupport logSupport, Provider<SingularitySchedulerStateCache> stateCacheProvider, SingularityHealthchecker healthchecker, DeployManager deployManager) {
    defaultResources = new Resources(mesosConfiguration.getDefaultCpus(), mesosConfiguration.getDefaultMemory(), 0);
    this.taskManager = taskManager;
    this.deployManager = deployManager;
    this.newTaskChecker = newTaskChecker;
    this.slaveAndRackManager = slaveAndRackManager;
    this.scheduler = scheduler;
    this.mesosTaskBuilder = mesosTaskBuilder;
    this.logSupport = logSupport;
    this.stateCacheProvider = stateCacheProvider;
    this.healthchecker = healthchecker;
  }

  @Override
  public void registered(SchedulerDriver driver, Protos.FrameworkID frameworkId, Protos.MasterInfo masterInfo) {
    LOG.info("Registered driver {}, with frameworkId {} and master {}", driver, frameworkId, masterInfo);
  }

  @Override
  public void reregistered(SchedulerDriver driver, Protos.MasterInfo masterInfo) {
    LOG.info("Reregistered driver {}, with master {}", driver, masterInfo);
  }

  @Override
  public void resourceOffers(SchedulerDriver driver, List<Protos.Offer> offers) {
    LOG.info("Received {} offer(s)", offers.size());

    final long start = System.currentTimeMillis();

    final SingularitySchedulerStateCache stateCache = stateCacheProvider.get();

    scheduler.checkForDecomissions(stateCache);
    scheduler.drainPendingQueue(stateCache);

    final Set<Protos.OfferID> acceptedOffers = Sets.newHashSetWithExpectedSize(offers.size());

    for (Protos.Offer offer : offers) {
      slaveAndRackManager.checkOffer(offer);
    }

    int numDueTasks = 0;

    try {
      final List<SingularityTaskRequest> taskRequests = scheduler.getDueTasks();

      for (SingularityTaskRequest taskRequest : taskRequests) {
        LOG.trace("Task {} is due", taskRequest.getPendingTask().getPendingTaskId());
      }

      numDueTasks = taskRequests.size();

      final List<SingularityOfferHolder> offerHolders = Lists.newArrayListWithCapacity(offers.size());

      for (Protos.Offer offer : offers) {
        offerHolders.add(new SingularityOfferHolder(offer, numDueTasks));
      }

      boolean addedTaskInLastLoop = true;

      while (!taskRequests.isEmpty() && addedTaskInLastLoop) {
        addedTaskInLastLoop = false;
        Collections.shuffle(offerHolders);

        for (SingularityOfferHolder offerHolder : offerHolders) {
          Optional<SingularityTask> accepted = match(taskRequests, stateCache, offerHolder);
          if (accepted.isPresent()) {
            offerHolder.addMatchedTask(accepted.get());
            addedTaskInLastLoop = true;
            taskRequests.remove(accepted.get().getTaskRequest());
          }

          if (taskRequests.isEmpty()) {
            break;
          }
        }
      }

      for (SingularityOfferHolder offerHolder : offerHolders) {
        if (!offerHolder.getAcceptedTasks().isEmpty()) {
          offerHolder.launchTasks(driver);

          acceptedOffers.add(offerHolder.getOffer().getId());
        } else {
          driver.declineOffer(offerHolder.getOffer().getId());
        }
      }

    } catch (Throwable t) {
      LOG.error("Received fatal error while accepting offers - will decline all available offers", t);

      for (Protos.Offer offer : offers) {
        if (acceptedOffers.contains(offer.getId())) {
          continue;
        }

        driver.declineOffer(offer.getId());
      }

      throw t;
    }

    LOG.info("Finished handling {} offer(s) ({}), {} accepted, {} declined, {} outstanding tasks", offers.size(), JavaUtils.duration(start), acceptedOffers.size(),
        offers.size() - acceptedOffers.size(), numDueTasks - acceptedOffers.size());
  }

  private Optional<SingularityTask> match(Collection<SingularityTaskRequest> taskRequests, SingularitySchedulerStateCache stateCache, SingularityOfferHolder offerHolder) {

    for (SingularityTaskRequest taskRequest : taskRequests) {
      Resources taskResources = defaultResources;

      if (taskRequest.getDeploy().getResources().isPresent()) {
        taskResources = taskRequest.getDeploy().getResources().get();
      }

      LOG.trace("Attempting to match task {} resources {} with remaining offer resources {}", taskRequest.getPendingTask().getPendingTaskId(), taskResources, offerHolder.getCurrentResources());

      final boolean matchesResources = MesosUtils.doesOfferMatchResources(taskResources, offerHolder.getCurrentResources());
      final SlaveMatchState slaveMatchState = slaveAndRackManager.doesOfferMatch(offerHolder.getOffer(), taskRequest, stateCache);

      if (matchesResources && slaveMatchState.isMatchAllowed()) {
        final SingularityTask task = mesosTaskBuilder.buildTask(offerHolder.getOffer(), offerHolder.getCurrentResources(), taskRequest, taskResources);

        LOG.trace("Accepted and built task {}", task);

        LOG.info("Launching task {} slot on slave {} ({})", task.getTaskId(), offerHolder.getOffer().getSlaveId().getValue(), offerHolder.getOffer().getHostname());

        taskManager.createTaskAndDeletePendingTask(task);

        stateCache.getActiveTaskIds().add(task.getTaskId());
        stateCache.getScheduledTasks().remove(taskRequest.getPendingTask());

        return Optional.of(task);
      } else {
        LOG.trace("Ignoring offer {} for task {}; matched resources: {}, slave match state: {}", offerHolder.getOffer().getId(), taskRequest.getPendingTask().getPendingTaskId(), matchesResources, slaveMatchState);
      }
    }

    return Optional.absent();
  }

  @Override
  public void offerRescinded(SchedulerDriver driver, Protos.OfferID offerId) {
    LOG.info("Offer {} rescinded", offerId);
  }

  @Override
  public void statusUpdate(SchedulerDriver driver, Protos.TaskStatus status) {
    final String taskId = status.getTaskId().getValue();

    long timestamp = System.currentTimeMillis();

    if (status.hasTimestamp()) {
      timestamp = (long) status.getTimestamp() * 1000;
    }

    LOG.debug("Task {} is now {} ({}) at {} ", taskId, status.getState(), status.getMessage(), timestamp);

    final SingularityTaskId taskIdObj = SingularityTaskId.fromString(taskId);
    final Optional<SingularityTask> maybeActiveTask = taskManager.getActiveTask(taskId);
    final ExtendedTaskState taskState = ExtendedTaskState.fromTaskState(status.getState());

    Optional<SingularityPendingDeploy> pendingDeploy = null;

    if (maybeActiveTask.isPresent() && status.getState() == TaskState.TASK_RUNNING) {
      pendingDeploy = deployManager.getPendingDeploy(taskIdObj.getRequestId());

      healthchecker.enqueueHealthcheck(maybeActiveTask.get(), pendingDeploy);
    }

    final SingularityTaskHistoryUpdate taskUpdate = new SingularityTaskHistoryUpdate(taskIdObj, timestamp, taskState, status.hasMessage() ? Optional.of(status.getMessage()) : Optional.<String> absent());
    final SingularityCreateResult taskHistoryUpdateCreateResult = taskManager.saveTaskHistoryUpdate(taskUpdate);

    logSupport.checkDirectory(taskIdObj);

    if (taskState.isDone()) {
      healthchecker.cancelHealthcheck(taskId);
      newTaskChecker.cancelNewTaskCheck(taskId);

      taskManager.deleteKilledRecord(taskIdObj);

      scheduler.handleCompletedTask(maybeActiveTask, taskIdObj, timestamp, taskState, taskHistoryUpdateCreateResult, stateCacheProvider.get());

      taskManager.deleteLastActiveTaskStatus(taskIdObj);
    } else {
      if (maybeActiveTask.isPresent()) {
        if (pendingDeploy == null) {
          pendingDeploy = deployManager.getPendingDeploy(taskIdObj.getRequestId());
        }

        if (!pendingDeploy.isPresent() || !pendingDeploy.get().getDeployMarker().getDeployId().equals(taskIdObj.getDeployId())) {
          newTaskChecker.enqueueNewTaskCheck(maybeActiveTask.get());
        }
      }

      taskManager.saveLastActiveTaskStatus(taskIdObj, status);
    }
  }

  @Override
  public void frameworkMessage(SchedulerDriver driver, Protos.ExecutorID executorId, Protos.SlaveID slaveId, byte[] data) {
    LOG.info("Framework message from executor {} on slave {} with data {}", executorId, slaveId, JavaUtils.toString(data));
  }

  @Override
  public void disconnected(SchedulerDriver driver) {
    LOG.warn("Scheduler/Driver disconnected");
  }

  @Override
  public void slaveLost(SchedulerDriver driver, Protos.SlaveID slaveId) {
    LOG.warn("Lost a slave {}", slaveId);

    slaveAndRackManager.slaveLost(slaveId);
  }

  @Override
  public void executorLost(SchedulerDriver driver, Protos.ExecutorID executorId, Protos.SlaveID slaveId, int status) {
    LOG.warn("Lost an executor {} on slave {} with status {}", executorId, slaveId, status);
  }

  @Override
  public void error(SchedulerDriver driver, String message) {
    LOG.warn("Error from mesos: {}", message);
  }

}
