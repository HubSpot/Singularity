package com.hubspot.singularity.scheduler;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityDriverManager;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestCleanup;
import com.hubspot.singularity.SingularityRequestCleanup.RequestCleanupType;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskCleanup.TaskCleanupType;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;

public class SingularityCleaner {
  
  private final static Logger LOG = LoggerFactory.getLogger(SingularityCleaner.class);
  
  private final TaskManager taskManager;
  private final RequestManager requestManager;
  private final SingularityDriverManager driverManager;
  
  private final long killTasksAfterNewestTaskIsAtLeastMillis;
  
  @Inject
  public SingularityCleaner(TaskManager taskManager, RequestManager requestManager, SingularityDriverManager driverManager, SingularityConfiguration configuration) {
    this.taskManager = taskManager;
    this.requestManager = requestManager;
    this.driverManager = driverManager;
    
    this.killTasksAfterNewestTaskIsAtLeastMillis = TimeUnit.SECONDS.toMillis(configuration.getKillDecomissionedTasksAfterNewTasksSeconds());
  }

  // TODO review this logic along with deploy logic., what types should work how?
  private boolean shouldKillTask(SingularityTaskCleanup taskCleanup, List<SingularityTaskId> activeTaskIds, List<SingularityTaskId> cleaningTasks) {
    if (taskCleanup.getCleanupTypeEnum() == TaskCleanupType.USER_REQUESTED) {
      LOG.debug(String.format("Killing a task %s because it was user requested", taskCleanup));
      return true;
    }
    
    // check to see if there are enough active tasks out there that have been active for long enough that we can safely shut this task down.
    Optional<SingularityRequest> request = requestManager.fetchRequest(taskCleanup.getTaskId().getRequestId());
    
    if (!request.isPresent()) {
      LOG.debug(String.format("Killing a task %s because the request was missing", taskCleanup));;
      return true;
    }
    
    List<SingularityTaskId> matchingTasks = SingularityTaskId.matchingAndNotIn(activeTaskIds, taskCleanup.getTaskId().getRequestId(), taskCleanup.getTaskId().getDeployId(), cleaningTasks);
    
    final long now = System.currentTimeMillis();
    long newestTaskDurationMillis = Long.MAX_VALUE;
    
    for (SingularityTaskId matchingTask : matchingTasks) {
      long taskDuration = now - matchingTask.getStartedAt();
      if (taskDuration < newestTaskDurationMillis) {
        newestTaskDurationMillis = taskDuration;
      }
    }
    
    boolean hasEnoughTasksWhichAreOldEnoughToKillThisTask = matchingTasks.size() >= request.get().getInstances().or(1) && newestTaskDurationMillis > killTasksAfterNewestTaskIsAtLeastMillis;
  
    LOG.debug(String.format("%s a task %s because there are %s active tasks (requirement %s) and the newest task is %sms old (must be at least %sms)", 
        hasEnoughTasksWhichAreOldEnoughToKillThisTask ? "Killing" : "Not killing", taskCleanup, matchingTasks.size(), request.get().getInstances(), newestTaskDurationMillis, killTasksAfterNewestTaskIsAtLeastMillis));
    
    return hasEnoughTasksWhichAreOldEnoughToKillThisTask;
  }
  
  private void drainRequestCleanupQueue() {
    final long start = System.currentTimeMillis();

    final List<SingularityRequestCleanup> cleanupRequests = requestManager.getCleanupRequests();
    
    LOG.debug(String.format("Request cleanup queue had %s requests", cleanupRequests.size()));
    
    if (cleanupRequests.isEmpty()) {
      return;
    }
    
    LOG.info(String.format("Cleaning up %s requests", cleanupRequests.size()));
    
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
          LOG.info(String.format("Not pausing %s, because it didn't exist in active requests", requestId));
        }
      } else if (requestCleanup.getCleanupTypeEnum() == RequestCleanupType.DELETING) {
        if (request.isPresent()) {        
          killTasks = false;
          LOG.info(String.format("Not cleaning %s, because it existed", requestId));
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
    
    LOG.info(String.format("Killed %s tasks (removed %s scheduled) in %sms", numTasksKilled, numScheduledTasksRemoved, System.currentTimeMillis() - start));
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
    
    LOG.debug(String.format("Task cleanup queue had %s tasks", cleanupTasks.size()));
    
    if (cleanupTasks.isEmpty()) {
      return;
    }
    
    final List<SingularityTaskId> cleaningTasks = Lists.newArrayListWithCapacity(cleanupTasks.size());
    for (SingularityTaskCleanup cleanupTask : cleanupTasks) {
      cleaningTasks.add(cleanupTask.getTaskId());
    }
    
    LOG.info(String.format("Cleaning up %s tasks", cleanupTasks.size()));
   
    final List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIds(); 
    
    int killedTasks = 0;
    
    for (SingularityTaskCleanup cleanupTask : cleanupTasks) {
      if (!isValidTask(cleanupTask)) {
        LOG.info(String.format("Couldn't find a matching active task for cleanup task %s, deleting..", cleanupTask));
        taskManager.deleteCleanupTask(cleanupTask.getTaskId().getId());
      } else if (shouldKillTask(cleanupTask, activeTaskIds, cleaningTasks)) {
        driverManager.kill(cleanupTask.getTaskId().getId());
        
        taskManager.deleteCleanupTask(cleanupTask.getTaskId().getId());
      
        killedTasks++;
      }
    }
    
    LOG.info(String.format("Killed %s tasks in %sms", killedTasks, System.currentTimeMillis() - start));
  }

}
