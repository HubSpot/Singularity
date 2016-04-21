package com.hubspot.singularity.mesos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Singleton;

import org.apache.mesos.Protos;
import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Scheduler;
import org.apache.mesos.SchedulerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.mesos.Resources;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.InvalidSingularityTaskIdException;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityMainModule;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.SingularityTaskStatusHolder;
import com.hubspot.singularity.SlaveMatchState;
import com.hubspot.singularity.config.CustomExecutorConfiguration;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
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

  private final Resources defaultResources;
  private final Resources defaultCustomExecutorResources;
  private final TaskManager taskManager;
  private final RequestManager requestManager;
  private final DeployManager deployManager;
  private final SingularityScheduler scheduler;
  private final SingularityConfiguration configuration;
  private final SingularityMesosTaskBuilder mesosTaskBuilder;
  private final SingularityMesosFrameworkMessageHandler messageHandler;
  private final SingularityHealthchecker healthchecker;
  private final SingularityNewTaskChecker newTaskChecker;
  private final SingularitySlaveAndRackManager slaveAndRackManager;
  private final SingularitySchedulerPriority schedulerPriority;
  private final SingularityLogSupport logSupport;
  private final SingularityTaskSizeOptimizer taskSizeOptimizer;

  private final SingularityExceptionNotifier exceptionNotifier;

  private final Provider<SingularitySchedulerStateCache> stateCacheProvider;
  private final String serverId;
  private final SchedulerDriverSupplier schedulerDriverSupplier;

  private final IdTranscoder<SingularityTaskId> taskIdTranscoder;

  @Inject
  SingularityMesosScheduler(MesosConfiguration mesosConfiguration, SingularityConfiguration configuration, TaskManager taskManager, SingularityScheduler scheduler, SingularitySlaveAndRackManager slaveAndRackManager,
      SingularitySchedulerPriority schedulerPriority, SingularityNewTaskChecker newTaskChecker, SingularityMesosTaskBuilder mesosTaskBuilder, SingularityLogSupport logSupport, RequestManager requestManager,
      Provider<SingularitySchedulerStateCache> stateCacheProvider, SingularityHealthchecker healthchecker, DeployManager deployManager, SingularityExceptionNotifier exceptionNotifier,SingularityMesosFrameworkMessageHandler messageHandler,
      @Named(SingularityMainModule.SERVER_ID_PROPERTY) String serverId, SchedulerDriverSupplier schedulerDriverSupplier, SingularityTaskSizeOptimizer taskSizeOptimizer, final IdTranscoder<SingularityTaskId> taskIdTranscoder, CustomExecutorConfiguration customExecutorConfiguration) {
    this.defaultResources = new Resources(mesosConfiguration.getDefaultCpus(), mesosConfiguration.getDefaultMemory(), 0, mesosConfiguration.getDefaultDisk());
    this.defaultCustomExecutorResources = new Resources(customExecutorConfiguration.getNumCpus(), customExecutorConfiguration.getMemoryMb(), 0, customExecutorConfiguration.getDiskMb());
    this.taskManager = taskManager;
    this.deployManager = deployManager;
    this.schedulerPriority = schedulerPriority;
    this.newTaskChecker = newTaskChecker;
    this.slaveAndRackManager = slaveAndRackManager;
    this.scheduler = scheduler;
    this.messageHandler = messageHandler;
    this.taskSizeOptimizer = taskSizeOptimizer;
    this.mesosTaskBuilder = mesosTaskBuilder;
    this.logSupport = logSupport;
    this.stateCacheProvider = stateCacheProvider;
    this.healthchecker = healthchecker;
    this.serverId = serverId;
    this.schedulerDriverSupplier = schedulerDriverSupplier;
    this.taskIdTranscoder = taskIdTranscoder;
    this.exceptionNotifier = exceptionNotifier;
    this.requestManager = requestManager;
    this.configuration = configuration;
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
  @Timed
  public void resourceOffers(SchedulerDriver driver, List<Protos.Offer> offers) {
    LOG.info("Received {} offer(s)", offers.size());

    for (Offer offer : offers) {
      LOG.debug("Received offer from {} ({}) for {} cpu(s), {} memory, {} ports, and {} disk", offer.getHostname(), offer.getSlaveId().getValue(), MesosUtils.getNumCpus(offer), MesosUtils.getMemory(offer),
          MesosUtils.getNumPorts(offer));
    }

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
      schedulerPriority.sortTaskRequestsInPriorityOrder(taskRequests);

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
          if (configuration.getMaxTasksPerOffer() > 0 && offerHolder.getAcceptedTasks().size() >= configuration.getMaxTasksPerOffer()) {
            LOG.trace("Offer {} is full ({}) - skipping", offerHolder.getOffer(), offerHolder.getAcceptedTasks().size());
            continue;
          }

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
      final Resources taskResources = taskRequest.getDeploy().getResources().or(defaultResources);

      // only factor in executor resources if we're running a custom executor
      final Resources executorResources = taskRequest.getDeploy().getCustomExecutorCmd().isPresent() ? taskRequest.getDeploy().getCustomExecutorResources().or(defaultCustomExecutorResources) : Resources.EMPTY_RESOURCES;

      final Resources totalResources = Resources.add(taskResources, executorResources);

      final List<Long> requestedPorts = new ArrayList<>();

      if (taskRequest.getDeploy().getContainerInfo().isPresent() && taskRequest.getDeploy().getContainerInfo().get().getDocker().isPresent()) {
        requestedPorts.addAll(taskRequest.getDeploy().getContainerInfo().get().getDocker().get().getLiteralHostPorts());
      }

      LOG.trace("Attempting to match task {} resources {} ({} for task + {} for executor) with remaining offer resources {}", taskRequest.getPendingTask().getPendingTaskId(), totalResources, taskResources, executorResources, offerHolder.getCurrentResources());

      final boolean matchesResources = MesosUtils.doesOfferMatchResources(totalResources, offerHolder.getCurrentResources(), requestedPorts);
      final SlaveMatchState slaveMatchState = slaveAndRackManager.doesOfferMatch(offerHolder.getOffer(), taskRequest, stateCache, getUpdatedRequest(taskRequest));

      if (matchesResources && slaveMatchState.isMatchAllowed()) {
        final SingularityTask task = mesosTaskBuilder.buildTask(offerHolder.getOffer(), offerHolder.getCurrentResources(), taskRequest, taskResources, executorResources);

        final SingularityTask zkTask = taskSizeOptimizer.getSizeOptimizedTask(task);

        LOG.trace("Accepted and built task {}", zkTask);

        LOG.info("Launching task {} slot on slave {} ({})", task.getTaskId(), offerHolder.getOffer().getSlaveId().getValue(), offerHolder.getOffer().getHostname());

        taskManager.createTaskAndDeletePendingTask(zkTask);

        schedulerPriority.notifyTaskLaunched(task.getTaskId());

        stateCache.getActiveTaskIds().add(task.getTaskId());
        stateCache.getScheduledTasks().remove(taskRequest.getPendingTask());

        return Optional.of(task);
      } else {
        LOG.trace("Ignoring offer {} on {} for task {}; matched resources: {}, slave match state: {}", offerHolder.getOffer().getId(), offerHolder.getOffer().getHostname(), taskRequest
            .getPendingTask().getPendingTaskId(), matchesResources, slaveMatchState);
      }
    }

    return Optional.absent();
  }

  private Optional<SingularityRequest> getUpdatedRequest(SingularityTaskRequest taskRequest) {
    final Optional<SingularityPendingDeploy> pendingDeploy = deployManager.getPendingDeploy(taskRequest.getRequest().getId());
    if (pendingDeploy.isPresent() && pendingDeploy.get().getDeployMarker().getDeployId().equals(taskRequest.getDeploy().getId())) {
      return pendingDeploy.get().getUpdatedRequest();
    } else {
      return Optional.absent();
    }
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
      exceptionNotifier.notify(e);
      LOG.error("Unexpected taskId {} ", taskId, e);
      return Optional.absent();
    }
  }

  private Optional<String> getStatusMessage(Protos.TaskStatus status, Optional<SingularityTask> task) {
    if (status.hasMessage() && !Strings.isNullOrEmpty(status.getMessage())) {
      return Optional.of(status.getMessage());
    } else if (status.hasReason() && status.getReason() == Protos.TaskStatus.Reason.REASON_MEMORY_LIMIT) {
      if (task.isPresent() && task.get().getTaskRequest().getDeploy().getResources().isPresent()) {
        if (task.get().getTaskRequest().getDeploy().getResources().get().getDiskMb() > 0) {
          return Optional.of(String.format("Task exceeded one or more memory limits (%s MB mem, %s MB disk).", task.get().getTaskRequest().getDeploy().getResources().get().getMemoryMb(), task.get().getTaskRequest().getDeploy().getResources().get().getDiskMb()));
        } else {
          return Optional.of(String.format("Task exceeded memory limit (%s MB mem).", task.get().getTaskRequest().getDeploy().getResources().get().getMemoryMb()));
        }

      }
      return Optional.of("Task exceeded memory limit");
    }

    return Optional.absent();
  }

  @Override
  @Timed
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

        Optional<SingularityRequestWithState> requestWithState = Optional.absent();

        if (taskState == ExtendedTaskState.TASK_RUNNING) {
          requestWithState = requestManager.getRequest(taskIdObj.getRequestId());
          healthchecker.enqueueHealthcheck(task.get(), pendingDeploy, requestWithState);
        }

        if (!pendingDeploy.isPresent() || !pendingDeploy.get().getDeployMarker().getDeployId().equals(taskIdObj.getDeployId())) {
          if (!requestWithState.isPresent()) {
            requestWithState = requestManager.getRequest(taskIdObj.getRequestId());
          }
          newTaskChecker.enqueueNewTaskCheck(task.get(), requestWithState, healthchecker);
        }
      } else {
        final String message = String.format("Task %s is active but is missing task data", taskId);
        exceptionNotifier.notify(message);
        LOG.error(message);
      }
    }

    final Optional<String> statusMessage = getStatusMessage(status, task);

    final SingularityTaskHistoryUpdate taskUpdate =
        new SingularityTaskHistoryUpdate(taskIdObj, timestamp, taskState, statusMessage, status.hasReason() ? Optional.of(status.getReason().name()) : Optional.<String>absent());
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
    LOG.info("Framework message from executor {} on slave {} with {} bytes of data", executorId, slaveId, data.length);

    messageHandler.handleMessage(executorId, slaveId, data);
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

  public boolean isConnected() {
    return schedulerDriverSupplier.get().isPresent();
  }

}
