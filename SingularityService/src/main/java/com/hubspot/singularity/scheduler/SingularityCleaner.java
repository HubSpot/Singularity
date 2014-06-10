package com.hubspot.singularity.scheduler;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.baragon.models.BaragonRequestState;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.LoadBalancerRequestType;
import com.hubspot.singularity.LoadBalancerRequestType.LoadBalancerRequestId;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDriverManager;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestCleanup;
import com.hubspot.singularity.SingularityRequestCleanup.RequestCleanupType;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.hooks.LoadBalancerClient;
import com.hubspot.singularity.scheduler.SingularityDeployHealthHelper.DeployHealth;

public class SingularityCleaner {
  
  private final static Logger LOG = LoggerFactory.getLogger(SingularityCleaner.class);
  
  private final TaskManager taskManager;
  private final DeployManager deployManager;
  private final RequestManager requestManager;
  private final SingularityDriverManager driverManager;
  private final SingularityDeployHealthHelper deployHealthHelper;
  private final LoadBalancerClient lbClient;
  
  private final long killScheduledTasksAfterDecomissionedMillis;
  
  @Inject
  public SingularityCleaner(TaskManager taskManager, SingularityDeployHealthHelper deployHealthHelper, DeployManager deployManager, RequestManager requestManager, 
      SingularityDriverManager driverManager, SingularityConfiguration configuration, LoadBalancerClient lbClient) {
    this.taskManager = taskManager;
    this.lbClient = lbClient;
    this.deployHealthHelper = deployHealthHelper;
    this.deployManager = deployManager;
    this.requestManager = requestManager;
    this.driverManager = driverManager;
    
    this.killScheduledTasksAfterDecomissionedMillis = TimeUnit.SECONDS.toMillis(configuration.getKillScheduledTasksWhichAreDecomissionedAfterSeconds());
  }
  
  private boolean shouldKillTask(SingularityTaskCleanup taskCleanup, List<SingularityTaskId> activeTaskIds, List<SingularityTaskId> cleaningTasks) {
    if (taskCleanup.getCleanupType().shouldKillInstantly()) {
      LOG.debug("Killing a task {} immediately because of its cleanup type", taskCleanup);
      return true;
    }
         
    // check to see if there are enough active tasks out there that have been active for long enough that we can safely shut this task down.
    final Optional<SingularityRequestWithState> requestWithState = requestManager.getRequest(taskCleanup.getTaskId().getRequestId());
    
    if (!requestWithState.isPresent()) {
      LOG.debug("Killing a task {} immediately because the request was missing", taskCleanup);
      return true;
    }
    
    if (requestWithState.get().getState() == RequestState.PAUSED) {
      LOG.debug("Killing a task {} immediately because the request was paused", taskCleanup);
      return true;
    }
    
    final SingularityRequest request = requestWithState.get().getRequest();
    
    if (request.isScheduled()) {
      final long taskDuration = System.currentTimeMillis() - taskCleanup.getTaskId().getStartedAt();
      final boolean tooOld = taskDuration > killScheduledTasksAfterDecomissionedMillis;
      
      LOG.debug("{} a scheduled task {} immediately because the task is {} old (max wait time is {})", tooOld ? "Killing" : "Not killing", taskCleanup, taskDuration, killScheduledTasksAfterDecomissionedMillis);
      
      return tooOld;
    }
    
    final String requestId = request.getId();
    
    final Optional<SingularityRequestDeployState> deployState = deployManager.getRequestDeployState(requestId);
    
    if (!deployState.isPresent() || !deployState.get().getActiveDeploy().isPresent()) {
      LOG.debug("Killing a task {} immediately because there is no active deploy state {}", taskCleanup, deployState);
      return true;
    }
    
    final String activeDeployId = deployState.get().getActiveDeploy().get().getDeployId();
    
    if (!taskCleanup.getTaskId().getDeployId().equals(activeDeployId)) {
      LOG.debug("Killing a task {} immediately because it is not part of the active deploy {}", taskCleanup, deployState.get().getActiveDeploy().get());
      return true;
    }
    
    final List<SingularityTaskId> matchingTasks = SingularityTaskId.matchingAndNotIn(activeTaskIds, taskCleanup.getTaskId().getRequestId(), taskCleanup.getTaskId().getDeployId(), cleaningTasks);
    
    if (matchingTasks.size() < request.getInstancesSafe()) {
      LOG.debug("Not killing a task {} yet, only {} matching out of a required {}", taskCleanup, matchingTasks.size(), request.getInstancesSafe());
      return false;
    }
    
    final Optional<SingularityDeploy> deploy = deployManager.getDeploy(requestId, activeDeployId);
    
    final DeployHealth deployHealth = deployHealthHelper.getDeployHealth(deploy, matchingTasks);
    
    switch (deployHealth) {
    case HEALTHY:
      LOG.debug("Killing a task {}, all tasks are healthy", taskCleanup);
      return true;
    case WAITING:
    case UNHEALTHY:
    default:
      LOG.debug("Not killing a task {}, waiting for new tasks to be healthy (current state: {})", taskCleanup, deployState);
      return false;
    }
  }
  
  private void drainRequestCleanupQueue() {
    final long start = System.currentTimeMillis();

    final List<SingularityRequestCleanup> cleanupRequests = requestManager.getCleanupRequests();
    
    if (cleanupRequests.isEmpty()) {
      LOG.trace("Request cleanup queue is empty");
      return;
    }

    LOG.info("Cleaning up {} requests", cleanupRequests.size());
    
    final List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIds();
    final List<SingularityPendingTask> pendingTasks = taskManager.getScheduledTasks();
    
    int numTasksKilled = 0;
    int numScheduledTasksRemoved = 0;
    
    for (SingularityRequestCleanup requestCleanup : cleanupRequests) {
      final String requestId = requestCleanup.getRequestId();
      final Optional<SingularityRequestWithState> requestWithState = requestManager.getRequest(requestId);
      
      boolean killTasks = true;
      
      if (requestCleanup.getCleanupType() == RequestCleanupType.PAUSING) {
        if (SingularityRequestWithState.isActive(requestWithState)) {
          killTasks = false;
          LOG.info("Not pausing {}, because it was {}", requestId, requestWithState.get().getState());
        }
      } else if (requestCleanup.getCleanupType() == RequestCleanupType.DELETING) {
        if (requestWithState.isPresent()) {        
          killTasks = false;
          LOG.info("Not cleaning {}, because it existed", requestId);
        }
      }
      
      if (killTasks) {        
        for (SingularityTaskId matchingTaskId : Iterables.filter(activeTaskIds, SingularityTaskId.matchingRequest(requestId))) {
          driverManager.kill(matchingTaskId.toString());
          numTasksKilled++;
        }
     
        for (SingularityPendingTask matchingTask : Iterables.filter(pendingTasks, SingularityPendingTask.matching(requestId))) {
          taskManager.deleteScheduledTask(matchingTask.getPendingTaskId().getId());
          numScheduledTasksRemoved++;
        }
      }
     
      requestManager.deleteCleanRequest(requestId);
    }
    
    LOG.info("Killed {} tasks (removed {} scheduled) in {}", numTasksKilled, numScheduledTasksRemoved, JavaUtils.duration(start));
  }
  
  public void drainCleanupQueue() {
    drainRequestCleanupQueue();
    drainTaskCleanupQueue();
    drainLBCleanupQueue();
  }
  
  private boolean isValidTask(SingularityTaskCleanup cleanupTask) {
    return taskManager.isActiveTask(cleanupTask.getTaskId().getId());
  }
  
  private void drainTaskCleanupQueue() {
    final long start = System.currentTimeMillis();

    final List<SingularityTaskCleanup> cleanupTasks = taskManager.getCleanupTasks();
    
    if (cleanupTasks.isEmpty()) {
      LOG.trace("Task cleanup queue is empty");
      return;
    }
    
    final List<SingularityTaskId> cleaningTasks = Lists.newArrayListWithCapacity(cleanupTasks.size());
    for (SingularityTaskCleanup cleanupTask : cleanupTasks) {
      cleaningTasks.add(cleanupTask.getTaskId());
    }
    
    LOG.info("Cleaning up {} tasks", cleanupTasks.size());
   
    final List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIds(); 
    
    int killedTasks = 0;
    
    for (SingularityTaskCleanup cleanupTask : cleanupTasks) {
      if (!isValidTask(cleanupTask)) {
        LOG.info("Couldn't find a matching active task for cleanup task {}, deleting..", cleanupTask);
        taskManager.deleteCleanupTask(cleanupTask.getTaskId().getId());
      } else if (shouldKillTask(cleanupTask, activeTaskIds, cleaningTasks) && checkLBStateAndShouldKillTask(cleanupTask)) {
        driverManager.kill(cleanupTask.getTaskId().getId());
        
        taskManager.deleteCleanupTask(cleanupTask.getTaskId().getId());
      
        killedTasks++;
      }
    }
    
    LOG.info("Killed {} tasks in {}", killedTasks, JavaUtils.duration(start));
  }

  private boolean checkLBStateAndShouldKillTask(SingularityTaskCleanup cleanupTask) {
    final long start = System.currentTimeMillis();
    
    CheckLBState checkLbState = checkLbState(cleanupTask.getTaskId());
    
    LOG.debug("TaskCleanup {} had LB state {} after {}", cleanupTask, checkLbState, JavaUtils.duration(start));
    
    switch (checkLbState) {
    case DONE:
    case NOT_LOAD_BALANCED:
    case MISSING_TASK:
      return true;
    case WAITING:
    }
    
    return false;
  }
  
  private enum CheckLBState {
    NOT_LOAD_BALANCED, MISSING_TASK, WAITING, DONE;
  }
  
  private CheckLBState checkLbState(SingularityTaskId taskId) {
    Optional<SingularityLoadBalancerUpdate> lbAddUpdate = taskManager.getLoadBalancerState(taskId, LoadBalancerRequestType.ADD);
    
    if (!lbAddUpdate.isPresent() || lbAddUpdate.get().getLoadBalancerState() != BaragonRequestState.SUCCESS) {
      return CheckLBState.NOT_LOAD_BALANCED;
    }
    
    Optional<SingularityLoadBalancerUpdate> maybeLbRemoveUpdate = taskManager.getLoadBalancerState(taskId, LoadBalancerRequestType.REMOVE);
    SingularityLoadBalancerUpdate lbRemoveUpdate = null;
    
    final LoadBalancerRequestId loadBalancerRequestId = new LoadBalancerRequestId(taskId.getId(), LoadBalancerRequestType.REMOVE);
      
    if (!maybeLbRemoveUpdate.isPresent() || maybeLbRemoveUpdate.get().getLoadBalancerState() == BaragonRequestState.UNKNOWN) {
      final Optional<SingularityTask> task = taskManager.getTask(taskId);
      
      if (!task.isPresent()) {
        LOG.error("Missing task {}", taskId);
        return CheckLBState.MISSING_TASK;
      }
      
      lbRemoveUpdate = lbClient.enqueue(loadBalancerRequestId, task.get().getTaskRequest().getRequest(), task.get().getTaskRequest().getDeploy(), Collections.<SingularityTask> emptyList(), Collections.singletonList(task.get()));
      
      taskManager.saveLoadBalancerState(taskId, LoadBalancerRequestType.REMOVE, lbRemoveUpdate);
    } else if (maybeLbRemoveUpdate.get().getLoadBalancerState() == BaragonRequestState.WAITING) {
      lbRemoveUpdate = lbClient.getState(loadBalancerRequestId);

      taskManager.saveLoadBalancerState(taskId, LoadBalancerRequestType.REMOVE, lbRemoveUpdate);
    } else {
      lbRemoveUpdate = maybeLbRemoveUpdate.get();
    }
    
    switch (lbRemoveUpdate.getLoadBalancerState()) {
    case FAILED:
    case CANCELED:
    case CANCELING:
      LOG.error("LB request {} is in an invalid, unexpected state {}", loadBalancerRequestId, lbRemoveUpdate.getLoadBalancerState());
    case SUCCESS:
      return CheckLBState.DONE;
    case UNKNOWN:
    case WAITING:
      LOG.trace("Waiting on LB cleanup request {}", loadBalancerRequestId);
    }
    
    return CheckLBState.WAITING;
  }
  
  private void drainLBCleanupQueue() {
    final long start = System.currentTimeMillis();

    final List<SingularityTaskId> lbCleanupTasks = taskManager.getLBCleanupTasks();
    
    if (lbCleanupTasks.isEmpty()) {
      LOG.trace("LB task cleanup queue is empty");
      return;
    }

    LOG.info("LB task cleanup queue had {} tasks", lbCleanupTasks.size());
    
    int cleanedTasks = 0;
    int ignoredTasks = 0;
    
    for (SingularityTaskId taskId : lbCleanupTasks) {
      final long checkStart = System.currentTimeMillis();
      
      final CheckLBState checkLbState = checkLbState(taskId);
      
      LOG.debug("LB cleanup for task {} had state {} after {}", taskId, checkLbState, JavaUtils.duration(checkStart));
      
      switch (checkLbState) {
      case WAITING:
        continue;
      case DONE:
      case MISSING_TASK:
        cleanedTasks++;
        break;
      case NOT_LOAD_BALANCED:
        ignoredTasks++;
      }
      
      taskManager.deleteLBCleanupTask(taskId);
    }
    
    LOG.info("LB cleaned {} tasks ({} left, {} obsolete) in {}", cleanedTasks, lbCleanupTasks.size() - (ignoredTasks + cleanedTasks), ignoredTasks, JavaUtils.duration(start));
  }
  
}
