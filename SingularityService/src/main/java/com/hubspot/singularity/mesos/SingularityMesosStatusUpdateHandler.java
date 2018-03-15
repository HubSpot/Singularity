package com.hubspot.singularity.mesos;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.mesos.v1.Protos;
import org.apache.mesos.v1.Protos.TaskState;
import org.apache.mesos.v1.Protos.TaskStatus.Reason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Meter;
import com.google.common.base.Strings;
import com.google.common.collect.Multiset;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityMainModule;
import com.hubspot.singularity.api.common.SingularityCreateResult;
import com.hubspot.singularity.api.request.SingularityPendingDeploy;
import com.hubspot.singularity.api.request.SingularityRequestWithState;
import com.hubspot.singularity.api.task.ExtendedTaskState;
import com.hubspot.singularity.api.task.SingularityTask;
import com.hubspot.singularity.api.task.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.api.task.SingularityTaskId;
import com.hubspot.singularity.api.task.SingularityTaskStatusHolder;
import com.hubspot.singularity.async.AsyncSemaphore;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.transcoders.IdTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityTranscoderException;
import com.hubspot.singularity.exceptions.InvalidSingularityTaskIdException;
import com.hubspot.singularity.helpers.MesosProtosUtils;
import com.hubspot.singularity.helpers.MesosUtils;
import com.hubspot.singularity.scheduler.SingularityHealthchecker;
import com.hubspot.singularity.scheduler.SingularityLeaderCache;
import com.hubspot.singularity.scheduler.SingularityNewTaskChecker;
import com.hubspot.singularity.scheduler.SingularityScheduler;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

@Singleton
public class SingularityMesosStatusUpdateHandler {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityMesosStatusUpdateHandler.class);

  private final TaskManager taskManager;
  private final DeployManager deployManager;
  private final RequestManager requestManager;
  private final IdTranscoder<SingularityTaskId> taskIdTranscoder;
  private final SingularityExceptionNotifier exceptionNotifier;
  private final SingularityHealthchecker healthchecker;
  private final SingularityNewTaskChecker newTaskChecker;
  private final SingularitySlaveAndRackManager slaveAndRackManager;
  private final SingularityMesosExecutorInfoSupport logSupport;
  private final SingularityScheduler scheduler;
  private final SingularityLeaderCache leaderCache;
  private final MesosProtosUtils mesosProtosUtils;
  private final String serverId;
  private final SingularitySchedulerLock schedulerLock;
  private final SingularityConfiguration configuration;
  private final Multiset<Protos.TaskStatus.Reason> taskLostReasons;
  private final Meter lostTasksMeter;
  private final ConcurrentHashMap<Long, Long> statusUpdateDeltas;

  private final ExecutorService statusUpdatesExecutor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("status-updates-%d").build());
  private final AsyncSemaphore<Boolean> statusUpdatesSemaphore;

  @Inject
  public SingularityMesosStatusUpdateHandler(TaskManager taskManager,
                                             DeployManager deployManager,
                                             RequestManager requestManager,
                                             IdTranscoder<SingularityTaskId> taskIdTranscoder,
                                             SingularityExceptionNotifier exceptionNotifier,
                                             SingularityHealthchecker healthchecker,
                                             SingularityNewTaskChecker newTaskChecker,
                                             SingularitySlaveAndRackManager slaveAndRackManager,
                                             SingularityMesosExecutorInfoSupport logSupport,
                                             SingularityScheduler scheduler,
                                             @Named(SingularityMainModule.SERVER_ID_PROPERTY) String serverId,
                                             SingularitySchedulerLock schedulerLock,
                                             SingularityConfiguration configuration,
                                             SingularityLeaderCache leaderCache,
                                             MesosProtosUtils mesosProtosUtils,
                                             @Named(SingularityMesosModule.TASK_LOST_REASONS_COUNTER) Multiset<Protos.TaskStatus.Reason> taskLostReasons,
                                             @Named(SingularityMainModule.LOST_TASKS_METER) Meter lostTasksMeter,
                                             @Named(SingularityMainModule.STATUS_UPDATE_DELTAS) ConcurrentHashMap<Long, Long> statusUpdateDeltas) {
    this.taskManager = taskManager;
    this.deployManager = deployManager;
    this.requestManager = requestManager;
    this.taskIdTranscoder = taskIdTranscoder;
    this.exceptionNotifier = exceptionNotifier;
    this.healthchecker = healthchecker;
    this.newTaskChecker = newTaskChecker;
    this.slaveAndRackManager = slaveAndRackManager;
    this.logSupport = logSupport;
    this.scheduler = scheduler;
    this.leaderCache = leaderCache;
    this.mesosProtosUtils = mesosProtosUtils;
    this.serverId = serverId;
    this.schedulerLock = schedulerLock;
    this.configuration = configuration;
    this.taskLostReasons = taskLostReasons;
    this.lostTasksMeter = lostTasksMeter;
    this.statusUpdateDeltas = statusUpdateDeltas;
    this.statusUpdatesSemaphore = AsyncSemaphore
        .newBuilder(() -> configuration.getMesosConfiguration().getStatusUpdateConcurrencyLimit())
        .withQueueSize(configuration.getMesosConfiguration().getMaxStatusUpdateQueueSize())
        .build();
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
      exceptionNotifier.notify(String.format("Unexpected taskId %s", taskId), e);
      LOG.error("Unexpected taskId {} ", taskId, e);
      return Optional.empty();
    }
  }

  private Optional<String> getStatusMessage(Protos.TaskStatus status, Optional<SingularityTask> task) {
    if (status.hasMessage() && !Strings.isNullOrEmpty(status.getMessage())) {
      return Optional.of(status.getMessage());
    } else if (status.hasReason() && status.getReason() == Reason.REASON_CONTAINER_LIMITATION_MEMORY) {
      if (task.isPresent() && task.get().getTaskRequest().getDeploy().getResources().isPresent()) {
        if (task.get().getTaskRequest().getDeploy().getResources().get().getDiskMb() > 0) {
          return Optional.of(String.format("Task exceeded one or more memory limits (%s MB mem, %s MB disk).", task.get().getTaskRequest().getDeploy().getResources().get().getMemoryMb(),
              task.get().getTaskRequest().getDeploy().getResources().get().getDiskMb()));
        } else {
          return Optional.of(String.format("Task exceeded memory limit (%s MB mem).", task.get().getTaskRequest().getDeploy().getResources().get().getMemoryMb()));
        }

      }
      return Optional.of("Task exceeded memory limit.");
    } else if (status.hasReason() && status.getReason() == Reason.REASON_CONTAINER_LIMITATION_DISK) {
      if (task.isPresent() && task.get().getTaskRequest().getDeploy().getResources().isPresent()) {
        return Optional.of(String.format("Task exceeded disk limit (%s MB disk).", task.get().getTaskRequest().getDeploy().getResources().get().getDiskMb()));
      } else {
        return Optional.of("Task exceeded disk limit.");
      }
    }

    return Optional.empty();
  }

  private void unsafeProcessStatusUpdate(Protos.TaskStatus status, SingularityTaskId taskIdObj) {
    final String taskId = status.getTaskId().getValue();

    long timestamp = System.currentTimeMillis();

    if (status.hasTimestamp()) {
      timestamp = (long) (status.getTimestamp() * 1000);
    }

    long now = System.currentTimeMillis();
    long delta = now - timestamp;

    LOG.debug("Update: task {} is now {} ({}) at {} (delta: {})", taskId, status.getState(), status.getMessage(), timestamp, JavaUtils.durationFromMillis(delta));
    statusUpdateDeltas.put(now, delta);

    final SingularityTaskStatusHolder newTaskStatusHolder = new SingularityTaskStatusHolder(taskIdObj, Optional.of(mesosProtosUtils.taskStatusFromProtos(status)), System.currentTimeMillis(), serverId, Optional.empty());
    final Optional<SingularityTaskStatusHolder> previousTaskStatusHolder = taskManager.getLastActiveTaskStatus(taskIdObj);
    final ExtendedTaskState taskState = MesosUtils.fromTaskState(status.getState());

    if (isDuplicateOrIgnorableStatusUpdate(previousTaskStatusHolder, newTaskStatusHolder)) {
      LOG.trace("Ignoring status update {} to {}", taskState, taskIdObj);
      saveNewTaskStatusHolder(taskIdObj, newTaskStatusHolder, taskState);
      return;
    }

    if (status.getState() == TaskState.TASK_LOST) {
      lostTasksMeter.mark();
      if (configuration.getDisasterDetection().isEnabled()) {
        taskLostReasons.add(status.getReason());
      }
    }

    final Optional<SingularityTask> task = taskManager.getTask(taskIdObj);

    final boolean isActiveTask = taskManager.isActiveTask(taskId);

    if (isActiveTask && !taskState.isDone()) {
      if (task.isPresent()) {
        final Optional<SingularityPendingDeploy> pendingDeploy = deployManager.getPendingDeploy(taskIdObj.getRequestId());

        Optional<SingularityRequestWithState> requestWithState = Optional.empty();

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
        new SingularityTaskHistoryUpdate(taskIdObj, timestamp, taskState, statusMessage, status.hasReason() ? Optional.of(status.getReason().name()) : Optional.empty(), Collections.emptyList());
    final SingularityCreateResult taskHistoryUpdateCreateResult = taskManager.saveTaskHistoryUpdate(taskUpdate);

    logSupport.checkDirectoryAndContainerId(taskIdObj);

    if (taskState.isDone()) {
      healthchecker.cancelHealthcheck(taskId);
      newTaskChecker.cancelNewTaskCheck(taskId);

      taskManager.deleteKilledRecord(taskIdObj);

      slaveAndRackManager.checkStateAfterFinishedTask(taskIdObj, status.getAgentId().getValue(), leaderCache);

      scheduler.handleCompletedTask(task, taskIdObj, isActiveTask, timestamp, taskState, taskHistoryUpdateCreateResult, status);
    }

    saveNewTaskStatusHolder(taskIdObj, newTaskStatusHolder, taskState);
  }

  public CompletableFuture<Boolean> processStatusUpdateAsync(Protos.TaskStatus status) {
    return statusUpdatesSemaphore.call(() -> CompletableFuture.supplyAsync(() -> {
        final String taskId = status.getTaskId().getValue();
        final Optional<SingularityTaskId> maybeTaskId = getTaskId(taskId);

        if (!maybeTaskId.isPresent()) {
          return false;
        }

        schedulerLock.runWithRequestLock(
            () -> unsafeProcessStatusUpdate(status, maybeTaskId.get()),
            maybeTaskId.get().getRequestId(),
            getClass().getSimpleName()
        );
        return true;
      }, statusUpdatesExecutor)
    );
  }
}
