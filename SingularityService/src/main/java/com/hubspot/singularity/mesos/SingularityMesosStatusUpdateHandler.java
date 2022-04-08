package com.hubspot.singularity.mesos;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.protos.MesosTaskState;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.InvalidSingularityTaskIdException;
import com.hubspot.singularity.LoadBalancerRequestType;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.Singularity;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.SingularityMainModule;
import com.hubspot.singularity.SingularityManagedThreadPoolFactory;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityPendingRequestBuilder;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskStatusHolder;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.async.ExecutorAndQueue;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.history.HistoryManager;
import com.hubspot.singularity.data.history.TaskHistoryHelper;
import com.hubspot.singularity.data.transcoders.IdTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityTranscoderException;
import com.hubspot.singularity.helpers.MesosProtosUtils;
import com.hubspot.singularity.helpers.MesosUtils;
import com.hubspot.singularity.hooks.LoadBalancerClient;
import com.hubspot.singularity.scheduler.SingularityHealthchecker;
import com.hubspot.singularity.scheduler.SingularityLeaderCache;
import com.hubspot.singularity.scheduler.SingularityNewTaskChecker;
import com.hubspot.singularity.scheduler.SingularityScheduler;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.apache.mesos.v1.Protos;
import org.apache.mesos.v1.Protos.TaskState;
import org.apache.mesos.v1.Protos.TaskStatus;
import org.apache.mesos.v1.Protos.TaskStatus.Reason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SingularityMesosStatusUpdateHandler {
  private static final Logger LOG = LoggerFactory.getLogger(
    SingularityMesosStatusUpdateHandler.class
  );

  private static final Set<MesosTaskState> ACTIVE_STATES = ImmutableSet.of(
    MesosTaskState.TASK_STAGING,
    MesosTaskState.TASK_STARTING,
    MesosTaskState.TASK_RUNNING
  );
  private static final String RESOURCE_MISMATCH_ERR =
    "required by task and its executor is more than available";

  private final TaskManager taskManager;
  private final DeployManager deployManager;
  private final RequestManager requestManager;
  private final IdTranscoder<SingularityTaskId> taskIdTranscoder;
  private final SingularityExceptionNotifier exceptionNotifier;
  private final SingularityHealthchecker healthchecker;
  private final SingularityNewTaskChecker newTaskChecker;
  private final SingularityAgentAndRackManager agentAndRackManager;
  private final SingularityMesosExecutorInfoSupport logSupport;
  private final SingularityScheduler scheduler;
  private final SingularityLeaderCache leaderCache;
  private final MesosProtosUtils mesosProtosUtils;
  private final String serverId;
  private final SingularitySchedulerLock schedulerLock;
  private final SingularityConfiguration configuration;
  private final Multiset<Protos.TaskStatus.Reason> taskLostReasons;
  private final Meter lostTasksMeter;
  private final Histogram statusUpdateDeltas;
  private final LoadBalancerClient lbClient;
  private final HistoryManager historyManager;

  private final ExecutorAndQueue statusUpdatesExecutor;

  @Inject
  public SingularityMesosStatusUpdateHandler(
    TaskManager taskManager,
    DeployManager deployManager,
    RequestManager requestManager,
    IdTranscoder<SingularityTaskId> taskIdTranscoder,
    SingularityExceptionNotifier exceptionNotifier,
    SingularityHealthchecker healthchecker,
    SingularityNewTaskChecker newTaskChecker,
    SingularityAgentAndRackManager agentAndRackManager,
    SingularityMesosExecutorInfoSupport logSupport,
    SingularityScheduler scheduler,
    @Named(SingularityMainModule.SERVER_ID_PROPERTY) String serverId,
    SingularitySchedulerLock schedulerLock,
    SingularityConfiguration configuration,
    SingularityLeaderCache leaderCache,
    MesosProtosUtils mesosProtosUtils,
    LoadBalancerClient lbClient,
    HistoryManager historyManager,
    SingularityManagedThreadPoolFactory threadPoolFactory,
    @Named(
      SingularityMesosModule.TASK_LOST_REASONS_COUNTER
    ) Multiset<Protos.TaskStatus.Reason> taskLostReasons,
    @Named(SingularityMainModule.LOST_TASKS_METER) Meter lostTasksMeter,
    @Named(SingularityMainModule.STATUS_UPDATE_DELTAS) Histogram statusUpdateDeltas
  ) {
    this.taskManager = taskManager;
    this.deployManager = deployManager;
    this.requestManager = requestManager;
    this.taskIdTranscoder = taskIdTranscoder;
    this.exceptionNotifier = exceptionNotifier;
    this.healthchecker = healthchecker;
    this.newTaskChecker = newTaskChecker;
    this.agentAndRackManager = agentAndRackManager;
    this.logSupport = logSupport;
    this.scheduler = scheduler;
    this.leaderCache = leaderCache;
    this.mesosProtosUtils = mesosProtosUtils;
    this.serverId = serverId;
    this.schedulerLock = schedulerLock;
    this.configuration = configuration;
    this.lbClient = lbClient;
    this.historyManager = historyManager;
    this.taskLostReasons = taskLostReasons;
    this.lostTasksMeter = lostTasksMeter;
    this.statusUpdateDeltas = statusUpdateDeltas;
    this.statusUpdatesExecutor =
      threadPoolFactory.get(
        "status-updates",
        configuration.getMesosConfiguration().getStatusUpdateConcurrencyLimit(),
        configuration.getMesosConfiguration().getMaxStatusUpdateQueueSize(),
        true
      );
  }

  private boolean isRecoveryStatusUpdate(
    Optional<SingularityTaskStatusHolder> previousTaskStatusHolder,
    Reason reason,
    ExtendedTaskState taskState,
    final SingularityTaskStatusHolder newTaskStatusHolder
  ) {
    if (
      !previousTaskStatusHolder.isPresent() && // Task was already removed from the active list
      !taskState.isDone() &&
      newTaskStatusHolder.getTaskStatus().isPresent() &&
      ACTIVE_STATES.contains(newTaskStatusHolder.getTaskStatus().get().getState())
    ) {
      LOG.warn(
        "Task {} recovered but may have already been replaced",
        newTaskStatusHolder.getTaskId()
      );
      return true;
    }
    return false;
  }

  /**
   * 1- we have a previous update, and this is a duplicate of it (ignore) 2- we don't have a
   * previous update, 2 cases: a - this task has already been destroyed (we can ignore it then) b -
   * we've never heard of this task (very unlikely since we first write a status into zk before we
   * launch a task)
   */
  private boolean isDuplicateOrIgnorableStatusUpdate(
    Optional<SingularityTaskStatusHolder> previousTaskStatusHolder,
    final SingularityTaskStatusHolder newTaskStatusHolder
  ) {
    if (!previousTaskStatusHolder.isPresent()) {
      return true;
    }

    if (!previousTaskStatusHolder.get().getTaskStatus().isPresent()) { // this is our launch state
      return false;
    }

    return (
      previousTaskStatusHolder.get().getTaskStatus().get().getState() ==
      newTaskStatusHolder.getTaskStatus().get().getState()
    );
  }

  private void saveNewTaskStatusHolder(
    SingularityTaskId taskIdObj,
    SingularityTaskStatusHolder newTaskStatusHolder,
    ExtendedTaskState taskState
  ) {
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

  private Optional<String> getStatusMessage(
    Protos.TaskStatus status,
    Optional<SingularityTask> task
  ) {
    if (status.hasMessage() && !Strings.isNullOrEmpty(status.getMessage())) {
      return Optional.of(status.getMessage());
    } else if (
      status.hasReason() &&
      status.getReason() == Reason.REASON_CONTAINER_LIMITATION_MEMORY
    ) {
      if (
        task.isPresent() &&
        task.get().getTaskRequest().getDeploy().getResources().isPresent()
      ) {
        if (
          task.get().getTaskRequest().getDeploy().getResources().get().getDiskMb() > 0
        ) {
          return Optional.of(
            String.format(
              "Task exceeded one or more memory limits (%s MB mem, %s MB disk).",
              task.get().getTaskRequest().getDeploy().getResources().get().getMemoryMb(),
              task.get().getTaskRequest().getDeploy().getResources().get().getDiskMb()
            )
          );
        } else {
          return Optional.of(
            String.format(
              "Task exceeded memory limit (%s MB mem).",
              task.get().getTaskRequest().getDeploy().getResources().get().getMemoryMb()
            )
          );
        }
      }
      return Optional.of("Task exceeded memory limit.");
    } else if (
      status.hasReason() && status.getReason() == Reason.REASON_CONTAINER_LIMITATION_DISK
    ) {
      if (
        task.isPresent() &&
        task.get().getTaskRequest().getDeploy().getResources().isPresent()
      ) {
        return Optional.of(
          String.format(
            "Task exceeded disk limit (%s MB disk).",
            task.get().getTaskRequest().getDeploy().getResources().get().getDiskMb()
          )
        );
      } else {
        return Optional.of("Task exceeded disk limit.");
      }
    }

    return Optional.empty();
  }

  private void relaunchTask(SingularityTask task) {
    SingularityPendingTask pendingTask = task.getTaskRequest().getPendingTask();

    SingularityPendingRequest pendingRequest = new SingularityPendingRequestBuilder()
      .setRequestId(task.getTaskRequest().getRequest().getId())
      .setDeployId(task.getTaskRequest().getDeploy().getId())
      .setPendingType(PendingType.RETRY)
      .setUser(pendingTask.getUser())
      .setRunId(pendingTask.getRunId())
      .setCmdLineArgsList(pendingTask.getCmdLineArgsList())
      .setSkipHealthchecks(pendingTask.getSkipHealthchecks())
      .setMessage(pendingTask.getMessage())
      .setResources(pendingTask.getResources())
      .setS3UploaderAdditionalFiles(pendingTask.getS3UploaderAdditionalFiles())
      .setRunAsUserOverride(pendingTask.getRunAsUserOverride())
      .setEnvOverrides(pendingTask.getEnvOverrides())
      .setExtraArtifacts(pendingTask.getExtraArtifacts())
      .setActionId(pendingTask.getActionId())
      .setRunAt(pendingTask.getPendingTaskId().getNextRunAt())
      .setTimestamp(System.currentTimeMillis())
      .build();

    requestManager.addToPendingQueue(pendingRequest);
  }

  private StatusUpdateResult unsafeProcessStatusUpdate(
    Protos.TaskStatus status,
    SingularityTaskId taskIdObj
  ) {
    final String taskId = status.getTaskId().getValue();

    long timestamp = System.currentTimeMillis();

    if (status.hasTimestamp()) {
      timestamp = (long) (status.getTimestamp() * 1000);
    }

    long now = System.currentTimeMillis();
    long delta = now - timestamp;

    LOG.info(
      "Update: task {} is now {} ({}) at {} (delta: {})",
      taskId,
      status.getState(),
      status.getMessage(),
      timestamp,
      JavaUtils.durationFromMillis(delta)
    );
    statusUpdateDeltas.update(delta);

    final SingularityTaskStatusHolder newTaskStatusHolder = new SingularityTaskStatusHolder(
      taskIdObj,
      Optional.of(mesosProtosUtils.taskStatusFromProtos(status)),
      System.currentTimeMillis(),
      serverId,
      Optional.<String>empty()
    );
    final Optional<SingularityTaskStatusHolder> previousTaskStatusHolder = taskManager.getLastActiveTaskStatus(
      taskIdObj
    );
    final ExtendedTaskState taskState = MesosUtils.fromTaskState(status.getState());

    if (
      taskState == ExtendedTaskState.TASK_ERROR &&
      status.getMessage() != null &&
      status.getMessage().contains(RESOURCE_MISMATCH_ERR)
    ) {
      LOG.error(
        "Possible duplicate resource allocation",
        new IllegalStateException(
          String.format(
            "Duplicate resource allocation for %s: %s",
            taskId,
            status.getMessage()
          )
        )
      );
    }

    if (
      isRecoveryStatusUpdate(
        previousTaskStatusHolder,
        status.getReason(),
        taskState,
        newTaskStatusHolder
      )
    ) {
      return tryRecoverTask(
        status,
        taskIdObj,
        taskId,
        newTaskStatusHolder,
        taskState,
        now
      );
    }

    // If a task is missing data in Singularity there is not much we can do to recover it
    Optional<SingularityTask> maybeTask = taskManager.getTask(taskIdObj);
    if (!maybeTask.isPresent()) {
      maybeTask = tryFindMissingTaskData(taskIdObj, taskId, taskState);
    }
    if (!maybeTask.isPresent()) {
      return handledMissingTaskData(
        taskIdObj,
        taskId,
        newTaskStatusHolder,
        taskState,
        now
      );
    }

    SingularityTask task = maybeTask.get();

    if (
      isDuplicateOrIgnorableStatusUpdate(previousTaskStatusHolder, newTaskStatusHolder)
    ) {
      LOG.trace("Ignoring status update {} to {}", taskState, taskIdObj);
      saveNewTaskStatusHolder(taskIdObj, newTaskStatusHolder, taskState);
      return StatusUpdateResult.IGNORED;
    }

    if (status.getState() == TaskState.TASK_LOST) {
      boolean isMesosFailure =
        status.getReason() == Reason.REASON_INVALID_OFFERS ||
        status.getReason() == Reason.REASON_AGENT_REMOVED ||
        status.getReason() == Reason.REASON_AGENT_RESTARTED ||
        status.getReason() == Reason.REASON_AGENT_UNKNOWN ||
        status.getReason() == Reason.REASON_MASTER_DISCONNECTED ||
        status.getReason() == Reason.REASON_AGENT_DISCONNECTED;

      RequestType requestType = task.getTaskRequest().getRequest().getRequestType();
      boolean isRelaunchable = requestType != null && !requestType.isLongRunning();

      if (isMesosFailure && isRelaunchable) {
        LOG.info("Relaunching lost task {}", task);
        relaunchTask(task);
      }
      lostTasksMeter.mark();
      if (configuration.getDisasterDetection().isEnabled()) {
        taskLostReasons.add(status.getReason());
      }
    }

    if (!taskState.isDone()) {
      final Optional<SingularityPendingDeploy> pendingDeploy = deployManager.getPendingDeploy(
        taskIdObj.getRequestId()
      );

      Optional<SingularityRequestWithState> requestWithState = Optional.empty();

      if (taskState == ExtendedTaskState.TASK_RUNNING) {
        requestWithState = requestManager.getRequest(taskIdObj.getRequestId());
        healthchecker.enqueueHealthcheck(task, pendingDeploy, requestWithState);
      }

      if (
        !pendingDeploy.isPresent() ||
        !pendingDeploy
          .get()
          .getDeployMarker()
          .getDeployId()
          .equals(taskIdObj.getDeployId())
      ) {
        if (!requestWithState.isPresent()) {
          requestWithState = requestManager.getRequest(taskIdObj.getRequestId());
        }
        newTaskChecker.enqueueNewTaskCheck(task, requestWithState, healthchecker);
      }
    }

    final Optional<String> statusMessage = getStatusMessage(status, Optional.of(task));

    final SingularityTaskHistoryUpdate taskUpdate = new SingularityTaskHistoryUpdate(
      taskIdObj,
      timestamp,
      taskState,
      statusMessage,
      status.hasReason() ? Optional.of(status.getReason().name()) : Optional.empty()
    );
    final SingularityCreateResult taskHistoryUpdateCreateResult = taskManager.saveTaskHistoryUpdate(
      taskUpdate
    );

    logSupport.checkDirectoryAndContainerId(taskIdObj);

    if (taskState.isDone()) {
      healthchecker.cancelHealthcheck(taskId);
      newTaskChecker.cancelNewTaskCheck(taskId);

      taskManager.deleteKilledRecord(taskIdObj);

      handleCompletedTaskState(
        status,
        taskIdObj,
        taskState,
        taskHistoryUpdateCreateResult,
        Optional.of(task),
        timestamp
      );
    }

    saveNewTaskStatusHolder(taskIdObj, newTaskStatusHolder, taskState);
    return StatusUpdateResult.DONE;
  }

  private StatusUpdateResult tryRecoverTask(
    Protos.TaskStatus status,
    SingularityTaskId taskIdObj,
    String taskId,
    SingularityTaskStatusHolder newTaskStatusHolder,
    ExtendedTaskState taskState,
    long now
  ) {
    LOG.info(
      "Found recovery status update with reason {} for task {}",
      status.getReason(),
      taskId
    );
    final Optional<SingularityTaskHistory> maybeTaskHistory = taskManager.getTaskHistory(
      taskIdObj
    );
    if (
      !maybeTaskHistory.isPresent() ||
      !maybeTaskHistory.get().getLastTaskUpdate().isPresent()
    ) {
      LOG.warn(
        "Task {} not found to recover, it may have already been persisted. Triggering a kill via mesos",
        taskIdObj
      );

      return StatusUpdateResult.KILL_TASK;
    } else if (status.getReason() == Reason.REASON_AGENT_REREGISTERED) {
      Optional<SingularityLoadBalancerUpdate> maybeLbUpdate = taskManager.getLoadBalancerState(
        taskIdObj,
        LoadBalancerRequestType.REMOVE
      );
      if (maybeLbUpdate.isPresent()) {
        LOG.info(
          "LB removal for recovered task {} was already started. Attempting to clear and start as new task",
          taskId
        );
        boolean canRecoverLbState = true;
        if (maybeLbUpdate.get().getLoadBalancerState().isInProgress()) {
          try {
            if (
              lbClient
                .getState(maybeLbUpdate.get().getLoadBalancerRequestId())
                .getLoadBalancerState()
                .isInProgress()
            ) {
              // We don't want to block here and wait for LB removal to finish in case it is stuck. Mark this task for cleaning
              canRecoverLbState = false;
            }
          } catch (Exception e) {
            LOG.warn("Could not verify LB state for {}", taskId, e);
            canRecoverLbState = false;
          }
        }
        if (
          canRecoverLbState &&
          deployManager
            .getActiveDeployId(taskIdObj.getRequestId())
            .map(d -> d.equals(taskIdObj.getDeployId()))
            .orElse(false) &&
          taskManager.reactivateTask(
            taskIdObj,
            taskState,
            newTaskStatusHolder,
            Optional.ofNullable(status.getMessage()),
            status.hasReason() ? Optional.of(status.getReason().name()) : Optional.empty()
          )
        ) {
          Optional<SingularityTask> maybeTask = taskManager.getTask(taskIdObj);
          Optional<SingularityRequestWithState> maybeRequest = requestManager.getRequest(
            taskIdObj.getRequestId()
          );
          if (
            maybeTask.isPresent() &&
            maybeRequest.isPresent() &&
            maybeRequest.get().getState().isRunnable()
          ) {
            LOG.info(
              "Task {} can be recovered. Clearing LB state and enqueuing check as new task",
              taskId
            );
            taskManager.clearLoadBalancerHistory(taskIdObj);
            newTaskChecker.enqueueCheckWithDelay(maybeTask.get(), 0, healthchecker);
            requestManager.addToPendingQueue(
              new SingularityPendingRequest(
                taskIdObj.getRequestId(),
                taskIdObj.getDeployId(),
                now,
                Optional.empty(),
                PendingType.TASK_RECOVERED,
                Optional.empty(),
                Optional.of(
                  String.format("Agent %s recovered", status.getAgentId().getValue())
                )
              )
            );
            return StatusUpdateResult.DONE;
          }
        } else {
          LOG.info("Could not recover task {}, will clean up", taskId);
          taskManager.createTaskCleanup(
            new SingularityTaskCleanup(
              Optional.empty(),
              TaskCleanupType.DECOMISSIONING,
              System.currentTimeMillis(),
              taskIdObj,
              Optional.of(
                "Agent re-registered after load balancer removal started. Task cannot be reactivated."
              ),
              Optional.empty(),
              Optional.empty()
            )
          );
          requestManager.addToPendingQueue(
            new SingularityPendingRequest(
              taskIdObj.getRequestId(),
              taskIdObj.getDeployId(),
              now,
              Optional.empty(),
              PendingType.TASK_RECOVERED,
              Optional.empty(),
              Optional.of(
                String.format("Agent %s recovered", status.getAgentId().getValue())
              )
            )
          );
          return StatusUpdateResult.DONE;
        }
      }
    }

    // Check tasks with no lb component or not yet removed from LB
    boolean reactivated =
      deployManager
        .getActiveDeployId(taskIdObj.getRequestId())
        .map(d -> d.equals(taskIdObj.getDeployId()))
        .orElse(false) &&
      taskManager.reactivateTask(
        taskIdObj,
        taskState,
        newTaskStatusHolder,
        Optional.ofNullable(status.getMessage()),
        status.hasReason() ? Optional.of(status.getReason().name()) : Optional.empty()
      );
    requestManager.addToPendingQueue(
      new SingularityPendingRequest(
        taskIdObj.getRequestId(),
        taskIdObj.getDeployId(),
        now,
        Optional.empty(),
        PendingType.TASK_RECOVERED,
        Optional.empty(),
        Optional.of(String.format("Agent %s recovered", status.getAgentId().getValue()))
      )
    );
    if (reactivated) {
      return StatusUpdateResult.DONE;
    } else {
      return StatusUpdateResult.KILL_TASK;
    }
  }

  private Optional<SingularityTask> tryFindMissingTaskData(
    SingularityTaskId taskIdObj,
    String taskId,
    ExtendedTaskState taskState
  ) {
    LOG.warn("Missing task data for {}, trying to recover", taskId);
    // If found in this first step, it was a bad zk write and everything should just work
    Optional<SingularityTask> maybeTask = taskManager.tryRepairTask(taskIdObj);
    if (!maybeTask.isPresent()) {
      // Ensure history manager calls cannot interrupt the status update path
      try {
        Optional<SingularityTaskHistory> maybeTaskHistory = historyManager.getTaskHistory(
          taskId
        );
        if (maybeTaskHistory.isPresent()) {
          maybeTask = maybeTaskHistory.map(SingularityTaskHistory::getTask);
          if (
            maybeTaskHistory
              .get()
              .getLastTaskUpdate()
              .map(SingularityTaskHistoryUpdate::getTaskState)
              .orElse(taskState)
              .isDone() &&
            !taskState.isDone()
          ) {
            // Don't bother with LB state/etc recovery, let the task get killed and replaced as a cleaner replacement
            LOG.info(
              "Recovered task {} was previously marked as done. Will not reactivate fully",
              taskId
            );
            taskManager.repairFoundTask(maybeTask.get());
            return Optional.empty();
          }
        }
      } catch (Exception e) {
        LOG.error("Could not fetch {} from history", taskId, e);
      }
      if (maybeTask.isPresent() && taskManager.repairFoundTask(maybeTask.get())) {
        LOG.info("Successfully repaired task data in zk for {}", taskId);
      }
    }
    // TODO - could we also try to fetch this from mesos agent somehow?
    return maybeTask;
  }

  private StatusUpdateResult handledMissingTaskData(
    SingularityTaskId taskIdObj,
    String taskId,
    SingularityTaskStatusHolder newTaskStatusHolder,
    ExtendedTaskState taskState,
    long now
  ) {
    if (taskState.isDone()) {
      LOG.info("No task data present for {} but task has finished, ignoring", taskId);
      saveNewTaskStatusHolder(taskIdObj, newTaskStatusHolder, taskState);
      requestManager.addToPendingQueue(
        new SingularityPendingRequest(
          taskIdObj.getRequestId(),
          taskIdObj.getDeployId(),
          now,
          Optional.empty(),
          PendingType.TASK_DONE,
          Optional.empty(),
          Optional.of(String.format("Unable to recover task %s", taskId))
        )
      );
      return StatusUpdateResult.DONE;
    } else {
      final String message = String.format(
        "Task %s is active but is missing task data, killing task",
        taskId
      );
      exceptionNotifier.notify(message);
      LOG.error(message);
      saveNewTaskStatusHolder(taskIdObj, newTaskStatusHolder, taskState);
      // Also save a task killed event to clean up active task list
      saveNewTaskStatusHolder(
        taskIdObj,
        newTaskStatusHolder,
        ExtendedTaskState.TASK_KILLED
      );
      requestManager.addToPendingQueue(
        new SingularityPendingRequest(
          taskIdObj.getRequestId(),
          taskIdObj.getDeployId(),
          now,
          Optional.empty(),
          PendingType.TASK_DONE,
          Optional.empty(),
          Optional.of(String.format("Unable to recover task %s", taskId))
        )
      );
      return StatusUpdateResult.KILL_TASK;
    }
  }

  private synchronized void handleCompletedTaskState(
    TaskStatus status,
    SingularityTaskId taskIdObj,
    ExtendedTaskState taskState,
    SingularityCreateResult taskHistoryUpdateCreateResult,
    Optional<SingularityTask> task,
    long timestamp
  ) {
    // Method synchronized to prevent race condition where two tasks complete at the same time but the leader cache holding the state
    // doesn't get updated between each task completion. If this were to happen, then agents would never transition from DECOMMISSIONING to
    // DECOMMISSIONED because each task state check thinks the other task is still running.
    agentAndRackManager.checkStateAfterFinishedTask(
      taskIdObj,
      status.getAgentId().getValue(),
      leaderCache
    );
    scheduler.handleCompletedTask(
      task,
      taskIdObj,
      timestamp,
      taskState,
      taskHistoryUpdateCreateResult,
      status
    );
  }

  public boolean hasRoomForMoreUpdates() {
    return (
      statusUpdatesExecutor.getQueue().size() < statusUpdatesExecutor.getQueueLimit()
    );
  }

  public CompletableFuture<StatusUpdateResult> processStatusUpdateAsync(
    Protos.TaskStatus status
  ) {
    return CompletableFuture.supplyAsync(
      () -> {
        final String taskId = status.getTaskId().getValue();
        final Optional<SingularityTaskId> maybeTaskId = getTaskId(taskId);

        if (!maybeTaskId.isPresent()) {
          return StatusUpdateResult.INVALID_TASK_ID;
        }

        return schedulerLock.runWithRequestLockAndReturn(
          () -> unsafeProcessStatusUpdate(status, maybeTaskId.get()),
          maybeTaskId.get().getRequestId(),
          getClass().getSimpleName()
        );
      },
      statusUpdatesExecutor.getExecutorService()
    );
  }

  public int getQueueSize() {
    return statusUpdatesExecutor.getQueue().size();
  }

  public double getQueueFullness() {
    LOG.info(
      "Queue size: {}, queue limit: {}, queue fullness: {}",
      statusUpdatesExecutor.getQueue().size(),
      statusUpdatesExecutor.getQueueLimit(),
      (double) statusUpdatesExecutor.getQueue().size() /
      statusUpdatesExecutor.getQueueLimit()
    );
    return (
      (double) statusUpdatesExecutor.getQueue().size() /
      statusUpdatesExecutor.getQueueLimit()
    );
  }
}
