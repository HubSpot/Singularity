package com.hubspot.singularity.scheduler;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployState;
import com.hubspot.singularity.SingularityDriverManager;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestCleanup;
import com.hubspot.singularity.SingularityRequestCleanup.RequestCleanupType;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.Utils;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.scheduler.SingularityDeployHealthHelper.DeployHealth;

public class SingularityCleaner {
  
  private final static Logger LOG = LoggerFactory.getLogger(SingularityCleaner.class);
  
  private final TaskManager taskManager;
  private final DeployManager deployManager;
  private final RequestManager requestManager;
  private final SingularityDriverManager driverManager;
  private final SingularityDeployHealthHelper deployHealthHelper;
  
  private final long killScheduledTasksAfterDecomissionedMillis;
  
  @Inject
  public SingularityCleaner(TaskManager taskManager, SingularityDeployHealthHelper deployHealthHelper, DeployManager deployManager, RequestManager requestManager, SingularityDriverManager driverManager, SingularityConfiguration configuration) {
    this.taskManager = taskManager;
    this.deployHealthHelper = deployHealthHelper;
    this.deployManager = deployManager;
    this.requestManager = requestManager;
    this.driverManager = driverManager;
    
    this.killScheduledTasksAfterDecomissionedMillis = TimeUnit.SECONDS.toMillis(configuration.getKillScheduledTasksWithAreDecomissionedAfterSeconds());
  }
  
  private boolean shouldKillTask(SingularityTaskCleanup taskCleanup, List<SingularityTaskId> activeTaskIds, List<SingularityTaskId> cleaningTasks) {
    if (taskCleanup.getCleanupTypeEnum().shouldKillInstantly()) {
      LOG.debug("Killing a task {} immediately because of its cleanup type", taskCleanup);
      return true;
    }
         
    // check to see if there are enough active tasks out there that have been active for long enough that we can safely shut this task down.
    final Optional<SingularityRequest> request = requestManager.fetchRequest(taskCleanup.getTaskId().getRequestId());
    
    if (!request.isPresent()) {
      LOG.debug("Killing a task {} immediately because the request was missing", taskCleanup);
      return true;
    }
    
    if (request.get().isScheduled()) {
      final long taskDuration = System.currentTimeMillis() - taskCleanup.getTaskId().getStartedAt();
      final boolean tooOld = taskDuration > killScheduledTasksAfterDecomissionedMillis;
      
      LOG.debug("{} a scheduled task {} immediately because the task is {} old (max wait time is {})", tooOld ? "Killing" : "Not killing", taskCleanup, taskDuration, killScheduledTasksAfterDecomissionedMillis);
      
      return tooOld;
    }
    
    final String requestId = request.get().getId();
    
    final Optional<SingularityDeployState> deployState = deployManager.getDeployState(requestId);
    
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
    
    if (matchingTasks.size() < request.get().getInstancesSafe()) {
      LOG.debug("Not killing a task {} yet, only {} matching out of a required {}", taskCleanup, matchingTasks.size(), request.get().getInstancesSafe());
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
    
    LOG.debug("Request cleanup queue had {} requests", cleanupRequests.size());
    
    if (cleanupRequests.isEmpty()) {
      return;
    }
    
    LOG.info("Cleaning up {} requests", cleanupRequests.size());
    
    final List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIds();
    final List<SingularityPendingTask> pendingTasks = taskManager.getScheduledTasks();
    
    int numTasksKilled = 0;
    int numScheduledTasksRemoved = 0;
    
    for (SingularityRequestCleanup requestCleanup : cleanupRequests) {
      final String requestId = requestCleanup.getRequestId();
      final Optional<SingularityRequest> request = requestManager.fetchRequest(requestId);
      
      boolean killTasks = true;
      
      if (requestCleanup.getCleanupTypeEnum() == RequestCleanupType.PAUSING) {
        if (request.isPresent()) {
          requestManager.pause(request.get());
        } else {
          killTasks = false;
          LOG.info("Not pausing {}, because it didn't exist in active requests", requestId);
        }
      } else if (requestCleanup.getCleanupTypeEnum() == RequestCleanupType.DELETING) {
        if (request.isPresent()) {        
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
    
    LOG.info("Killed {} tasks (removed {} scheduled) in {}", numTasksKilled, numScheduledTasksRemoved, Utils.duration(start));
  }
  
  public void drainCleanupQueue() {
    drainRequestCleanupQueue();
    drainTaskCleanupQueue();
  }
  
  private boolean isValidTask(SingularityTaskCleanup cleanupTask) {
    return taskManager.isActiveTask(cleanupTask.getTaskId().getId());
  }
  
  private void drainTaskCleanupQueue() {
    final long start = System.currentTimeMillis();

    final List<SingularityTaskCleanup> cleanupTasks = taskManager.getCleanupTasks();
    
    LOG.debug("Task cleanup queue had {} tasks", cleanupTasks.size());
    
    if (cleanupTasks.isEmpty()) {
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
      } else if (shouldKillTask(cleanupTask, activeTaskIds, cleaningTasks)) {
        driverManager.kill(cleanupTask.getTaskId().getId());
        
        taskManager.deleteCleanupTask(cleanupTask.getTaskId().getId());
      
        killedTasks++;
      }
    }
    
    LOG.info("Killed {} tasks in {}", killedTasks, Utils.duration(start));
  }

}
