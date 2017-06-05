package com.hubspot.singularity.scheduler;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.models.BaragonRequestState;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.LoadBalancerRequestType;
import com.hubspot.singularity.LoadBalancerRequestType.LoadBalancerRequestId;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityAbort.AbortReason;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.SingularityLoadBalancerUpdate.LoadBalancerMethod;
import com.hubspot.singularity.SingularityMainModule;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskHealthcheckResult;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskHistoryUpdate.SimplifiedTaskState;
import com.hubspot.singularity.SingularityTaskShellCommandRequestId;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DisasterManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.hooks.LoadBalancerClient;
import com.hubspot.singularity.scheduler.SingularityDeployHealthHelper.DeployHealth;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

/**
 * Handles tasks we need to check for staleness | load balancer state, etc - tasks that are not part of a deploy. ie, new replacement tasks.
 * Since we are making changes to these tasks, either killing them or blessing them, we don't have to do it actually as part of a lock.
 * b/c we will use a queue to kill them.
 */
@Singleton
public class SingularityNewTaskChecker {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityNewTaskChecker.class);

  private final SingularityConfiguration configuration;
  private final TaskManager taskManager;
  private final RequestManager requestManager;
  private final LoadBalancerClient lbClient;

  private final Map<String, Future<?>> taskIdToCheck;

  private final ScheduledExecutorService executorService;

  private final SingularityAbort abort;
  private final SingularityExceptionNotifier exceptionNotifier;
  private final SingularityDeployHealthHelper deployHealthHelper;
  private final DisasterManager disasterManager;

  @Inject
  public SingularityNewTaskChecker(@Named(SingularityMainModule.NEW_TASK_THREADPOOL_NAME) ScheduledExecutorService executorService, RequestManager requestManager,
                                   SingularityConfiguration configuration, LoadBalancerClient lbClient, TaskManager taskManager, SingularityExceptionNotifier exceptionNotifier, SingularityAbort abort,
                                   SingularityDeployHealthHelper deployHealthHelper, DisasterManager disasterManager) {
    this.configuration = configuration;
    this.requestManager = requestManager;
    this.taskManager = taskManager;
    this.lbClient = lbClient;
    this.abort = abort;

    this.taskIdToCheck = Maps.newConcurrentMap();

    this.executorService = executorService;

    this.exceptionNotifier = exceptionNotifier;
    this.deployHealthHelper = deployHealthHelper;
    this.disasterManager = disasterManager;
  }

  private boolean hasHealthcheck(SingularityTask task, Optional<SingularityRequestWithState> requestWithState) {
    if (disasterManager.isDisabled(SingularityAction.RUN_HEALTH_CHECKS)) {
      return false;
    }
    if (!task.getTaskRequest().getDeploy().getHealthcheck().isPresent()) {
      return false;
    }

    if (task.getTaskRequest().getPendingTask().getSkipHealthchecks().or(Boolean.FALSE)) {
      return false;
    }

    if (requestWithState.isPresent() && requestWithState.get().getRequest().getSkipHealthchecks().or(Boolean.FALSE)) {
      return false;
    }

    return true;
  }

  private long getKillAfterUnhealthyMillis() {
    return TimeUnit.SECONDS.toMillis(configuration.getKillAfterTasksDoNotRunDefaultSeconds());
  }

  private int getDelaySeconds(SingularityTask task, Optional<SingularityRequestWithState> requestWithState) {
    int delaySeconds = configuration.getNewTaskCheckerBaseDelaySeconds();

    if (hasHealthcheck(task, requestWithState)) {
      Optional<Integer> maybeStartupDelay = task.getTaskRequest().getDeploy().getHealthcheck().get().getStartupDelaySeconds().or(configuration.getStartupDelaySeconds());
      if (maybeStartupDelay.isPresent()) {
        return maybeStartupDelay.get();
      }
    } else if (task.getTaskRequest().getRequest().isLoadBalanced()) {
      return delaySeconds;
    }

    delaySeconds += task.getTaskRequest().getDeploy().getDeployHealthTimeoutSeconds().or(configuration.getDeployHealthyBySeconds());

    return delaySeconds;
  }

  @Timed
  // should only be called on tasks that are new and not part of a pending deploy.
  public void enqueueNewTaskCheck(SingularityTask task, Optional<SingularityRequestWithState> requestWithState, SingularityHealthchecker healthchecker) {
    if (taskIdToCheck.containsKey(task.getTaskId().getId())) {
      LOG.trace("Already had a newTaskCheck for task {}", task.getTaskId());
      return;
    }

    int delaySeconds = getDelaySeconds(task, requestWithState);

    enqueueCheckWithDelay(task, delaySeconds, healthchecker);
  }

  @VisibleForTesting
  Collection<Future<?>> getTaskCheckFutures() {
    return taskIdToCheck.values();
  }

  public void runNewTaskCheckImmediately(SingularityTask task, SingularityHealthchecker healthchecker) {
    final String taskId = task.getTaskId().getId();

    LOG.info("Requested immediate task check for {}", taskId);

    CancelState cancelState = cancelNewTaskCheck(taskId);

    if (cancelState == CancelState.NOT_CANCELED) {
      LOG.debug("Task {} check was already done, not running again", taskId);
      return;
    } else if (cancelState == CancelState.NOT_PRESENT) {
      LOG.trace("Task {} check was not present, not running immediately as it is assumed to be part of an active deploy", taskId);
      return;
    }

    Future<?> future = executorService.submit(getTaskCheck(task, healthchecker));

    taskIdToCheck.put(taskId, future);
  }

  public static enum CancelState {
    NOT_PRESENT, CANCELED, NOT_CANCELED;
  }

  public CancelState cancelNewTaskCheck(String taskId) {
    Future<?> future = taskIdToCheck.remove(taskId);

    if (future == null) {
      return CancelState.NOT_PRESENT;
    }

    boolean canceled = future.cancel(false);

    LOG.trace("Canceling new task check ({}) for task {}", canceled, taskId);

    if (canceled) {
      return CancelState.CANCELED;
    } else {
      return CancelState.NOT_CANCELED;
    }
  }

  private Runnable getTaskCheck(final SingularityTask task, final SingularityHealthchecker healthchecker) {
    return new Runnable() {

      @Override
      public void run() {
        try {
          Optional<SingularityRequestWithState> requestWithState = requestManager.getRequest(task.getTaskId().getRequestId());

          if (!requestWithState.isPresent()) {
            LOG.info("Ignoring task check for {}, missing request {}", task.getTaskId(), task.getTaskId().getRequestId());
            return;
          }

          boolean shouldReschedule = checkTask(task, requestWithState, healthchecker);

          if (shouldReschedule) {
            reEnqueueCheck(task, healthchecker);
          } else {
            taskIdToCheck.remove(task.getTaskId().getId());
          }
        } catch (Throwable t) {
          LOG.error("Uncaught throwable in task check for task {}, re-enqueing", task, t);
          exceptionNotifier.notify(String.format("Error in task check (%s)", t.getMessage()), t, ImmutableMap.of("taskId", task.getTaskId().toString()));

          reEnqueueCheckOrAbort(task, healthchecker);
        }
      }
    };
  }

  private void reEnqueueCheckOrAbort(SingularityTask task, SingularityHealthchecker healthchecker) {
    try {
      reEnqueueCheck(task, healthchecker);
    } catch (Throwable t) {
      LOG.error("Uncaught throwable re-enqueuing task check for task {}, aborting", task, t);
      exceptionNotifier.notify(String.format("Error in task check (%s)", t.getMessage()), t, ImmutableMap.of("taskId", task.getTaskId().toString()));
      abort.abort(AbortReason.UNRECOVERABLE_ERROR, Optional.of(t));
    }
  }

  private void reEnqueueCheck(SingularityTask task, SingularityHealthchecker healthchecker) {
    enqueueCheckWithDelay(task, configuration.getCheckNewTasksEverySeconds(), healthchecker);
  }

  private void enqueueCheckWithDelay(final SingularityTask task, long delaySeconds, SingularityHealthchecker healthchecker) {
    LOG.trace("Enqueuing a new task check for task {} with delay {}", task.getTaskId(), DurationFormatUtils.formatDurationHMS(TimeUnit.SECONDS.toMillis(delaySeconds)));

    ScheduledFuture<?> future = executorService.schedule(getTaskCheck(task, healthchecker), delaySeconds, TimeUnit.SECONDS);

    taskIdToCheck.put(task.getTaskId().getId(), future);
  }

  public enum CheckTaskState {
    UNHEALTHY_KILL_TASK, OBSOLETE, CHECK_IF_OVERDUE, LB_IN_PROGRESS_CHECK_AGAIN, HEALTHY;
  }

  private boolean checkTask(SingularityTask task, Optional<SingularityRequestWithState> requestWithState, SingularityHealthchecker healthchecker) {
    final long start = System.currentTimeMillis();

    final CheckTaskState state = getTaskState(task, requestWithState, healthchecker);

    LOG.debug("Got task state {} for task {} in {}", state, task.getTaskId(), JavaUtils.duration(start));

    switch (state) {
      case CHECK_IF_OVERDUE:
        if (isOverdue(task)) {
          LOG.info("Killing {} because it did not become healthy after {}", task.getTaskId(), JavaUtils.durationFromMillis(getKillAfterUnhealthyMillis()));

          taskManager.createTaskCleanup(new SingularityTaskCleanup(Optional.<String> absent(), TaskCleanupType.OVERDUE_NEW_TASK, System.currentTimeMillis(),
              task.getTaskId(), Optional.of(String.format("Task did not become healthy after %s", JavaUtils.durationFromMillis(getKillAfterUnhealthyMillis()))), Optional.<String>absent(), Optional.<SingularityTaskShellCommandRequestId>absent()));
          return false;
        } else {
          return true;
        }
      case LB_IN_PROGRESS_CHECK_AGAIN:
        return true;
      case UNHEALTHY_KILL_TASK:
        LOG.info("Killing {} because it failed healthchecks", task.getTaskId());

        taskManager.createTaskCleanup(new SingularityTaskCleanup(Optional.<String> absent(), TaskCleanupType.UNHEALTHY_NEW_TASK, System.currentTimeMillis(),
            task.getTaskId(), Optional.of("Task is not healthy"), Optional.<String> absent(), Optional.<SingularityTaskShellCommandRequestId>absent()));
        return false;
      case HEALTHY:
      case OBSOLETE:
        return false;
    }

    return false;
  }

  @VisibleForTesting
  CheckTaskState getTaskState(SingularityTask task, Optional<SingularityRequestWithState> requestWithState, SingularityHealthchecker healthchecker) {
    if (!taskManager.isActiveTask(task.getTaskId().getId())) {
      return CheckTaskState.OBSOLETE;
    }

    SimplifiedTaskState taskState = SingularityTaskHistoryUpdate.getCurrentState(taskManager.getTaskHistoryUpdates(task.getTaskId()));

    switch (taskState) {
      case DONE:
        return CheckTaskState.OBSOLETE;
      case WAITING:
      case UNKNOWN:
        return CheckTaskState.CHECK_IF_OVERDUE;
      case RUNNING:
        break;
    }

    if (hasHealthcheck(task, requestWithState)) {
      Optional<SingularityTaskHealthcheckResult> maybeHealthCheck = taskManager.getLastHealthcheck(task.getTaskId());

      DeployHealth health = deployHealthHelper.getTaskHealth(task.getTaskRequest().getDeploy(), false, maybeHealthCheck, task.getTaskId());
      switch (health) {
        case WAITING:
          healthchecker.checkHealthcheck(task);
          return CheckTaskState.CHECK_IF_OVERDUE;
        case UNHEALTHY:
          taskManager.clearStartupHealthchecks(task.getTaskId());
          return CheckTaskState.UNHEALTHY_KILL_TASK;
        case HEALTHY:
          taskManager.clearStartupHealthchecks(task.getTaskId());
          break;
      }
    }

    // task is running + has succeeded healthcheck if available.
    if (!task.getTaskRequest().getRequest().isLoadBalanced()) {
      return CheckTaskState.HEALTHY;
    }

    Optional<SingularityLoadBalancerUpdate> lbUpdate = taskManager.getLoadBalancerState(task.getTaskId(), LoadBalancerRequestType.ADD);
    SingularityLoadBalancerUpdate newLbUpdate = null;

    final LoadBalancerRequestId loadBalancerRequestId = new LoadBalancerRequestId(task.getTaskId().getId(), LoadBalancerRequestType.ADD, Optional.<Integer> absent());
    boolean taskCleaning = taskManager.getCleanupTaskIds().contains(task.getTaskId());


    if ((!lbUpdate.isPresent() || unknownNotRemoving(lbUpdate.get())) && !taskCleaning) {
      taskManager.saveLoadBalancerState(task.getTaskId(), LoadBalancerRequestType.ADD,
          new SingularityLoadBalancerUpdate(BaragonRequestState.UNKNOWN, loadBalancerRequestId, Optional.<String> absent(), System.currentTimeMillis(), LoadBalancerMethod.PRE_ENQUEUE, Optional.<String> absent()));

      newLbUpdate = lbClient.enqueue(loadBalancerRequestId, task.getTaskRequest().getRequest(), task.getTaskRequest().getDeploy(), Collections.singletonList(task), Collections.<SingularityTask> emptyList());
    } else {
      Optional<CheckTaskState> maybeCheckTaskState = checkLbState(lbUpdate.get().getLoadBalancerState());

      if (maybeCheckTaskState.isPresent()) {
        return maybeCheckTaskState.get();
      }

      newLbUpdate = lbClient.getState(loadBalancerRequestId);
    }

    taskManager.saveLoadBalancerState(task.getTaskId(), LoadBalancerRequestType.ADD, newLbUpdate);

    Optional<CheckTaskState> maybeCheckTaskState = checkLbState(newLbUpdate.getLoadBalancerState());

    if (maybeCheckTaskState.isPresent()) {
      return maybeCheckTaskState.get();
    }

    return CheckTaskState.LB_IN_PROGRESS_CHECK_AGAIN;
  }

  private Optional<CheckTaskState> checkLbState(BaragonRequestState lbState) {
    switch (lbState) {
      case SUCCESS:
        return Optional.of(CheckTaskState.HEALTHY);
      case CANCELED:
      case FAILED:
      case INVALID_REQUEST_NOOP:
        return Optional.of(CheckTaskState.UNHEALTHY_KILL_TASK);
      case CANCELING:
      case UNKNOWN:
      case WAITING:
        break;
    }

    return Optional.absent();
  }

  private boolean isOverdue(SingularityTask task) {
    final long taskDuration = System.currentTimeMillis() - task.getTaskId().getStartedAt();

    final boolean isOverdue = taskDuration > getKillAfterUnhealthyMillis();

    if (isOverdue) {
      LOG.debug("Task {} is overdue (duration: {}), allowed limit {}", task.getTaskId(), JavaUtils.durationFromMillis(taskDuration), JavaUtils.durationFromMillis(getKillAfterUnhealthyMillis()));
    }

    return isOverdue;
  }

  private boolean unknownNotRemoving(SingularityLoadBalancerUpdate update) {
    return update.getLoadBalancerState() == BaragonRequestState.UNKNOWN
        && update.getLoadBalancerRequestId().getRequestType() != LoadBalancerRequestType.REMOVE;
  }

}
