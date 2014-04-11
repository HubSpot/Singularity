package com.hubspot.singularity.scheduler;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.LoadBalancerRequestType;
import com.hubspot.singularity.LoadBalancerState;
import com.hubspot.singularity.SingularityCloseable;
import com.hubspot.singularity.SingularityCloser;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskCleanup.TaskCleanupType;
import com.hubspot.singularity.SingularityTaskHealthcheckResult;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskHistoryUpdate.SimplifiedTaskState;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.hooks.LoadBalancerClient;

public class SingularityNewTaskChecker implements SingularityCloseable {
  
  private final static Logger LOG = LoggerFactory.getLogger(SingularityNewTaskChecker.class);

  private final SingularityConfiguration configuration;
  private final TaskManager taskManager;
  private final SingularityCloser closer;
  private final LoadBalancerClient lbClient;
  private final long killAfterUnhealthyMillis;
  
  private final Map<String, ScheduledFuture<?>> taskIdToCheck;
  
  private final ScheduledExecutorService executorService;
  
  // only tasks we need to check for staleness | load balancer state, etc. is tasks that are not part of a deploy. ie, new replacement tasks.
  // since we are making changes to these tasks, either killing them or blessing them, we don't have to do it actually as part of alock.
  // b/c we will use q to kill them. we sould assume everythign in here is safe to be in here.
  
  @Inject
  public SingularityNewTaskChecker(SingularityConfiguration configuration, LoadBalancerClient lbClient, TaskManager taskManager, SingularityCloser closer) {
    this.configuration = configuration;
    this.taskManager = taskManager;
    this.lbClient = lbClient;
    this.closer = closer;
    
    this.taskIdToCheck = Maps.newConcurrentMap();
    this.killAfterUnhealthyMillis = TimeUnit.SECONDS.toMillis(configuration.getKillAfterTasksDoNotRunDefaultSeconds());
    
    this.executorService = Executors.newScheduledThreadPool(configuration.getCheckNewTasksScheduledThreads(), new ThreadFactoryBuilder().setNameFormat("SingularityNewTaskChecker-%d").build());
  }
  
  @Override
  public void close() {
    closer.shutdown(getClass().getName(), executorService, 1);
  }

  private boolean hasHealthcheck(SingularityTask task) {
    return task.getTaskRequest().getDeploy().getHealthcheckUri().isPresent();
  }
  
  private int getDelaySeconds(SingularityTask task) {
    int delaySeconds = 1; // TODO base buffer
    
    if (hasHealthcheck(task)) {
      delaySeconds += task.getTaskRequest().getDeploy().getHealthcheckIntervalSeconds().or(configuration.getHealthcheckIntervalSeconds());
    }
    
    delaySeconds += task.getTaskRequest().getDeploy().getDeployHealthTimeoutSeconds().or(configuration.getDeployHealthyBySeconds());
  
    return delaySeconds;
  }
  
  // should only be called on tasks that are new and not part of a pending deploy.
  public void enqueueNewTaskCheck(SingularityTask task) {
    if (taskIdToCheck.containsKey(task.getTaskId())) {
      LOG.trace(String.format("Already had a newTaskCheck for task for task %s", task.getTaskId()));
      return;
    }
    
    int delaySeconds = getDelaySeconds(task);
    
    enqueueCheckWithDelay(task, delaySeconds);
  }
   
  public void cancelNewTaskCheck(String taskId) {
    ScheduledFuture<?> future = taskIdToCheck.get(taskId);
    
    if (future == null) {
      return;
    }
    
    boolean canceled = future.cancel(false);
    
    LOG.trace(String.format("Canceling new task check (%s) for task %s", canceled, taskId));
  }
  
  private void enqueueCheckWithDelay(final SingularityTask task, long delaySeconds) {
    LOG.trace(String.format("Enqueing a new task check for task %s with delay %s", task.getTaskId(), DurationFormatUtils.formatDurationHMS(TimeUnit.SECONDS.toMillis(delaySeconds))));
  
    ScheduledFuture<?> future = executorService.schedule(new Runnable() {
      
      @Override
      public void run() {
        taskIdToCheck.remove(task.getTaskId().getId());
        
        try {
          checkTask(task);
        } catch (Throwable t) {
          LOG.error("Uncaught throwable in task check for task {}", task, t);
        }
      }
      
    }, delaySeconds, TimeUnit.SECONDS);
    
    taskIdToCheck.put(task.getTaskId().getId(), future);
  }
  
  private enum CheckTaskState {
    UNHEALTHY_KILL_TASK, OBSOLETE, CHECK_IF_OVERDUE, LB_IN_PROGRESS_CHECK_AGAIN, HEALTHY; 
  }
  
  private void checkTask(SingularityTask task) {
    final long start = System.currentTimeMillis();
    
    final CheckTaskState state = getTaskState(task);
    
    LOG.debug("Got task state {} for task {} in {}", state, task.getTaskId(), JavaUtils.duration(start));
    
    boolean reEnqueue = false;
    
    switch (state) {
    case HEALTHY:
    case OBSOLETE:
      break;
    case CHECK_IF_OVERDUE:
      if (isOverdue(task)) {
        taskManager.createCleanupTask(new SingularityTaskCleanup(Optional.<String> absent(), TaskCleanupType.OVERDUE_NEW_TASK, System.currentTimeMillis(), task.getTaskId()));
        break;
      } // otherwise, reEnqueue
    case LB_IN_PROGRESS_CHECK_AGAIN:
      reEnqueue = true;
      break;
    case UNHEALTHY_KILL_TASK:
      taskManager.createCleanupTask(new SingularityTaskCleanup(Optional.<String> absent(), TaskCleanupType.UNHEALTHY_NEW_TASK, System.currentTimeMillis(), task.getTaskId()));
      break;
    }
    
    if (reEnqueue) {
      LOG.debug("Re-enqueueing a check for task {}", task.getTaskId());
      
      enqueueCheckWithDelay(task, configuration.getCheckNewTasksEverySeconds());
    }
  }
  
  private CheckTaskState getTaskState(SingularityTask task) {
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
    }
    
    if (hasHealthcheck(task)) {
      Optional<SingularityTaskHealthcheckResult> healthCheck = taskManager.getHealthcheckResult(task.getTaskId());
      
      if (!healthCheck.isPresent()) {
        return CheckTaskState.CHECK_IF_OVERDUE;
      }
      
      if (healthCheck.get().isFailed()) {
        return CheckTaskState.UNHEALTHY_KILL_TASK;
      }
    }
    
    // task is running + has succeeded healthcheck if available.
    if (!task.getTaskRequest().getRequest().isLoadBalanced()) {
      return CheckTaskState.HEALTHY;
    } 
    
    Optional<LoadBalancerState> lbState = taskManager.getLoadBalancerState(task.getTaskId(), LoadBalancerRequestType.ADD);
    
    if (!lbState.isPresent()) {
      lbState = lbClient.enqueue(task.getTaskId().getId(), Collections.singletonList(task), Collections.<SingularityTask> emptyList());
    } else {
      Optional<CheckTaskState> maybeCheckTaskState = checkLbState(lbState.get());
      
      if (maybeCheckTaskState.isPresent()) {
        return maybeCheckTaskState.get();
      }
      
      lbState = lbClient.getState(task.getTaskId().getId());
    }
    
    if (lbState.isPresent()) {
      taskManager.saveLoadBalancerState(task.getTaskId(), LoadBalancerRequestType.ADD, lbState);
      
      Optional<CheckTaskState> maybeCheckTaskState = checkLbState(lbState.get());
      
      if (maybeCheckTaskState.isPresent()) {
        return maybeCheckTaskState.get();
      }
    }
    
    return CheckTaskState.LB_IN_PROGRESS_CHECK_AGAIN;
  }
  
  private Optional<CheckTaskState> checkLbState(LoadBalancerState lbState) {
    switch (lbState) {
    case SUCCESS:
      return Optional.of(CheckTaskState.HEALTHY);
    case CANCELED:
    case FAILED:
      return Optional.of(CheckTaskState.UNHEALTHY_KILL_TASK);
    case CANCELING:
    case WAITING:
      break;
    }
    
    return Optional.absent();
  }
  
  private boolean isOverdue(SingularityTask task) {
    final long taskDuration = System.currentTimeMillis() - task.getTaskId().getStartedAt();
    
    final boolean isOverdue = taskDuration > killAfterUnhealthyMillis;
  
    if (isOverdue) {
      LOG.debug("Task {} is overdue (duration: {}), allowed limit {}", task.getTaskId(), JavaUtils.durationFromMillis(taskDuration), JavaUtils.durationFromMillis(killAfterUnhealthyMillis));
    }
    
    return isOverdue;
  }
  
}
