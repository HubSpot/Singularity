package com.hubspot.singularity.mesos;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import org.apache.mesos.Protos;
import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Scheduler;
import org.apache.mesos.SchedulerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.InvalidSingularityTaskIdException;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityMainModule;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.SingularityTaskStatusHolder;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.transcoders.IdTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityTranscoderException;
import com.hubspot.singularity.scheduler.SingularityHealthchecker;
import com.hubspot.singularity.scheduler.SingularityNewTaskChecker;
import com.hubspot.singularity.scheduler.SingularityScheduler;
import com.hubspot.singularity.scheduler.SingularitySchedulerPriority;
import com.hubspot.singularity.scheduler.SingularitySchedulerStateCache;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

@Singleton
public class SingularityMesosScheduler implements Scheduler {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityMesosScheduler.class);

  private final TaskManager taskManager;
  private final DeployManager deployManager;
  private final SingularityScheduler scheduler;
  private final SingularityHealthchecker healthchecker;
  private final SingularityNewTaskChecker newTaskChecker;
  private final SingularitySlaveAndRackManager slaveAndRackManager;
  private final SingularityResourceScheduler resourceScheduler;
  private final SingularitySchedulerPriority schedulerPriority;
  private final SingularityLogSupport logSupport;

  private final SingularityExceptionNotifier exceptionNotifier;

  private final Provider<SingularitySchedulerStateCache> stateCacheProvider;
  private final String serverId;
  private final SchedulerDriverSupplier schedulerDriverSupplier;

  private final IdTranscoder<SingularityTaskId> taskIdTranscoder;

  @Inject
  SingularityMesosScheduler(TaskManager taskManager, SingularityScheduler scheduler, SingularitySlaveAndRackManager slaveAndRackManager, SingularityResourceScheduler resourceScheduler,
      SingularitySchedulerPriority schedulerPriority, SingularityNewTaskChecker newTaskChecker, SingularityLogSupport logSupport,
      Provider<SingularitySchedulerStateCache> stateCacheProvider, SingularityHealthchecker healthchecker, DeployManager deployManager, SingularityExceptionNotifier exceptionNotifier,
      @Named(SingularityMainModule.SERVER_ID_PROPERTY) String serverId, SchedulerDriverSupplier schedulerDriverSupplier, final IdTranscoder<SingularityTaskId> taskIdTranscoder) {

    this.taskManager = taskManager;
    this.deployManager = deployManager;
    this.schedulerPriority = schedulerPriority;
    this.newTaskChecker = newTaskChecker;
    this.slaveAndRackManager = slaveAndRackManager;
    this.resourceScheduler = resourceScheduler;
    this.scheduler = scheduler;
    this.logSupport = logSupport;
    this.stateCacheProvider = stateCacheProvider;
    this.healthchecker = healthchecker;
    this.serverId = serverId;
    this.schedulerDriverSupplier = schedulerDriverSupplier;
    this.taskIdTranscoder = taskIdTranscoder;
    this.exceptionNotifier = exceptionNotifier;
  }

  @Override
  public void registered(SchedulerDriver driver, Protos.FrameworkID frameworkId, Protos.MasterInfo masterInfo) {
    LOG.info("Registered driver {}, with frameworkId {} and master {}", driver, frameworkId, masterInfo);
    schedulerDriverSupplier.setSchedulerDriver(driver);
  }

  @Override
  public void reregistered(SchedulerDriver driver, Protos.MasterInfo masterInfo) {
    LOG.info("Reregistered driver {}, with master {}", driver, masterInfo);
    schedulerDriverSupplier.setSchedulerDriver(driver);
  }

  @Override
  public void resourceOffers(SchedulerDriver driver, List<Protos.Offer> offers) {
    LOG.info("Received {} offer(s)", offers.size());

    for (Offer offer : offers) {
      LOG.debug("Received offer from {} ({}) for {} cpu(s), {} memory, and {} ports", offer.getHostname(), offer.getSlaveId().getValue(), MesosUtils.getNumCpus(offer), MesosUtils.getMemory(offer),
          MesosUtils.getNumPorts(offer));
    }

    final Set<Protos.OfferID> acceptedOffers = Sets.newHashSetWithExpectedSize(offers.size());
    final long start = System.currentTimeMillis();
    final SingularitySchedulerStateCache stateCache = stateCacheProvider.get();
    int numDueTasks = 0;

    scheduler.checkForDecomissions(stateCache);
    scheduler.drainPendingQueue(stateCache);

    for (Protos.Offer offer : offers) {
      slaveAndRackManager.checkOffer(offer);
    }

    try {
      final List<SingularityTaskRequest> taskRequests = scheduler.getDueTasks();
      schedulerPriority.sortTaskRequestsInPriorityOrder(taskRequests);
      numDueTasks = taskRequests.size();

      List<SingularityOfferHolder> offerHolders = resourceScheduler.processOffers(stateCache, taskRequests, driver, offers);

      for (SingularityOfferHolder offerHolder : offerHolders) {
        if (!offerHolder.getAcceptedTasks().isEmpty()) {
          launchTasks(stateCache, driver, offerHolder.getAcceptedTasks(), offerHolder.getOffer());
          numDueTasks -= offerHolder.getAcceptedTasks().size();
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

  private void launchTasks(SingularitySchedulerStateCache stateCache, SchedulerDriver driver, List<SingularityTask> tasks, Offer offer) {
    final List<Protos.TaskInfo> toLaunch = Lists.newArrayListWithCapacity(tasks.size());
    final List<SingularityTaskId> taskIds = Lists.newArrayListWithCapacity(tasks.size());

    for (SingularityTask task : tasks) {
      taskManager.createTaskAndDeletePendingTask(task);
      schedulerPriority.notifyTaskLaunched(task.getTaskId());
      stateCache.getActiveTaskIds().add(task.getTaskId());
      stateCache.getScheduledTasks().remove(task.getTaskRequest().getPendingTask());
      taskIds.add(task.getTaskId());
      toLaunch.add(task.getMesosTask());
      LOG.trace("Launching {} mesos task: {}", task.getTaskId(), task.getMesosTask());
    }

    Protos.Status initialStatus = driver.launchTasks(ImmutableList.of(offer.getId()), toLaunch);
    LOG.info("{} tasks ({}) launched with status {}", taskIds.size(), taskIds, initialStatus);
  }

  @Override
  public void offerRescinded(SchedulerDriver driver, Protos.OfferID offerId) {
    LOG.info("Offer {} rescinded", offerId);
  }


  /**
   * 1- we have a previous update, and this is a duplicate of it (ignore) 2- we don't have a
   * previous update, 2 cases: a - this task has already been destroyed (we can ignore it then) b -
   * we've never heard of this task (very unlikely since we first write a status into zk before we
   * launch a task)
   */
  private boolean isDuplicateOrIgnorableStatusUpdate(Optional<SingularityTaskStatusHolder> previousTaskStatusHolder, final SingularityTaskStatusHolder newTaskStatusHolder) {
    if (!previousTaskStatusHolder.isPresent()) {
      return true;
    }

    if (!previousTaskStatusHolder.get().getTaskStatus().isPresent()) { // this is our launch state
      return false;
    }

    return previousTaskStatusHolder.get().getTaskStatus().get().getState() == newTaskStatusHolder.getTaskStatus().get().getState();
  }

  private void saveNewTaskStatusHolder(SingularityTaskId taskIdObj, SingularityTaskStatusHolder newTaskStatusHolder, ExtendedTaskState taskState) {
    if (taskState.isDone()) {
      taskManager.deleteLastActiveTaskStatus(taskIdObj);
    } else {
      taskManager.saveLastActiveTaskStatus(newTaskStatusHolder);
    }
  }

  private Optional<SingularityTaskId> getTaskId(String taskId) {
    try {
      return Optional.of(taskIdTranscoder.fromString(taskId));
    } catch (InvalidSingularityTaskIdException | SingularityTranscoderException e) {
      exceptionNotifier.notify(e, Collections.<String, String>emptyMap());
      LOG.error("Unexpected taskId {} ", taskId, e);
      return Optional.absent();
    }
  }

  @Override
  public void statusUpdate(SchedulerDriver driver, Protos.TaskStatus status) {
    final String taskId = status.getTaskId().getValue();

    long timestamp = System.currentTimeMillis();

    if (status.hasTimestamp()) {
      timestamp = (long) (status.getTimestamp() * 1000);
    }

    LOG.debug("Task {} is now {} ({}) at {} ", taskId, status.getState(), status.getMessage(), timestamp);

    final Optional<SingularityTaskId> maybeTaskId = getTaskId(taskId);

    if (!maybeTaskId.isPresent()) {
      return;
    }

    final SingularityTaskId taskIdObj = maybeTaskId.get();

    final SingularityTaskStatusHolder newTaskStatusHolder = new SingularityTaskStatusHolder(taskIdObj, Optional.of(status), System.currentTimeMillis(), serverId, Optional.<String>absent());
    final Optional<SingularityTaskStatusHolder> previousTaskStatusHolder = taskManager.getLastActiveTaskStatus(taskIdObj);
    final ExtendedTaskState taskState = ExtendedTaskState.fromTaskState(status.getState());

    if (isDuplicateOrIgnorableStatusUpdate(previousTaskStatusHolder, newTaskStatusHolder)) {
      LOG.trace("Ignoring status update {} to {}", taskState, taskIdObj);
      saveNewTaskStatusHolder(taskIdObj, newTaskStatusHolder, taskState);
      return;
    }

    final Optional<SingularityTask> task = taskManager.getTask(taskIdObj);

    final boolean isActiveTask = taskManager.isActiveTask(taskId);

    if (isActiveTask && !taskState.isDone()) {
      if (task.isPresent()) {
        final Optional<SingularityPendingDeploy> pendingDeploy = deployManager.getPendingDeploy(taskIdObj.getRequestId());

        if (taskState == ExtendedTaskState.TASK_RUNNING) {
          healthchecker.enqueueHealthcheck(task.get(), pendingDeploy);
        }

        if (!pendingDeploy.isPresent() || !pendingDeploy.get().getDeployMarker().getDeployId().equals(taskIdObj.getDeployId())) {
          newTaskChecker.enqueueNewTaskCheck(task.get());
        }
      } else {
        final String message = String.format("Task %s is active but is missing task data", taskId);
        exceptionNotifier.notify(message, Collections.<String, String>emptyMap());
        LOG.error(message);
      }
    }

    final SingularityTaskHistoryUpdate taskUpdate =
        new SingularityTaskHistoryUpdate(taskIdObj, timestamp, taskState, status.hasMessage() ? Optional.of(status.getMessage()) : Optional.<String>absent());
    final SingularityCreateResult taskHistoryUpdateCreateResult = taskManager.saveTaskHistoryUpdate(taskUpdate);

    logSupport.checkDirectory(taskIdObj);

    if (taskState.isDone()) {
      healthchecker.cancelHealthcheck(taskId);
      newTaskChecker.cancelNewTaskCheck(taskId);

      taskManager.deleteKilledRecord(taskIdObj);

      SingularitySchedulerStateCache stateCache = stateCacheProvider.get();

      slaveAndRackManager.checkStateAfterFinishedTask(taskIdObj, status.getSlaveId().getValue(), stateCache);

      scheduler.handleCompletedTask(task, taskIdObj, isActiveTask, timestamp, taskState, taskHistoryUpdateCreateResult, stateCache);
    }

    saveNewTaskStatusHolder(taskIdObj, newTaskStatusHolder, taskState);
  }

  @Override
  public void frameworkMessage(SchedulerDriver driver, Protos.ExecutorID executorId, Protos.SlaveID slaveId, byte[] data) {
    LOG.info("Framework message from executor {} on slave {} with data {}", executorId, slaveId, new String(data, UTF_8));
  }

  @Override
  public void disconnected(SchedulerDriver driver) {
    schedulerDriverSupplier.setSchedulerDriver(null);
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
