package com.hubspot.singularity.scheduler;

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityDriverManager;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;

public class SingularityScheduler {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityScheduler.class);
  
  private final TaskManager taskManager;
  private final RequestManager requestManager;
  private final SingularityDriverManager driverManager;
  
  @Inject
  public SingularityScheduler(TaskManager taskManager, RequestManager requestManager, SingularityDriverManager driverManager) {
    this.taskManager = taskManager;
    this.requestManager = requestManager;
    this.driverManager = driverManager;
  }
  
  public void drainCleanupQueue() {
    final long start = System.currentTimeMillis();

    final List<String> cleanupRequests = requestManager.getCleanupRequestNames();
    
    LOG.info(String.format("Cleanup queue had %s requests", cleanupRequests.size()));
    
    if (cleanupRequests.isEmpty()) {
      return;
    }
    
    final List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIds();
    final List<SingularityPendingTaskId> pendingTaskIds = taskManager.getScheduledTasks();
    
    int numTasksKilled = 0;
    int numScheduledTasksRemoved = 0;
    
    for (String requestName : cleanupRequests) {
      if (!requestManager.fetchRequest(requestName).isPresent()) {
        
        for (SingularityTaskId matchingTaskId : SingularityTaskId.filter(activeTaskIds, requestName)) {
          driverManager.kill(matchingTaskId.toString());
          numTasksKilled++;
        }
     
        for (SingularityPendingTaskId pendingTaskId : pendingTaskIds) {
          if (pendingTaskId.getName().equals(requestName)) {
            taskManager.deleteScheduledTask(pendingTaskId.toString());
            numScheduledTasksRemoved++;
          }
        }
        
      } else {
        LOG.info(String.format("Not cleaning %s, it existed", requestName));
      }
     
      requestManager.deleteCleanRequest(requestName);
    }
    
    
    LOG.info(String.format("Killed %s tasks (removed %s scheduled) in %sms", numTasksKilled, numScheduledTasksRemoved, System.currentTimeMillis() - start));
  }
  
  public void drainPendingQueue(final List<SingularityTaskId> activeTaskIds) {
    final long start = System.currentTimeMillis();
    
    final List<String> pendingRequests = requestManager.getPendingRequestNames();
    
    LOG.info(String.format("Pending queue had %s requests", pendingRequests.size()));
    
    if (pendingRequests.isEmpty()) {
      return;
    }
    
    final List<SingularityPendingTaskId> scheduledTasks = taskManager.getScheduledTasks();
    
    int numScheduledTasks = 0;
    int obsoleteRequests = 0;
    
    for (String pendingRequest : pendingRequests) {
      Optional<SingularityRequest> maybeRequest = requestManager.fetchRequest(pendingRequest);
      
      if (maybeRequest.isPresent()) {
        numScheduledTasks += scheduleTasks(scheduledTasks, activeTaskIds, maybeRequest.get()).size();
      } else {
        obsoleteRequests++;
      }
      
      requestManager.deletePendingRequest(pendingRequest);
    }
    
    LOG.info(String.format("Scheduled %s requests (%s obsolete) in %sms", numScheduledTasks, obsoleteRequests, System.currentTimeMillis() - start));
  }
  
  public List<SingularityTaskRequest> getDueTasks() {
    final List<SingularityPendingTaskId> tasks = taskManager.getScheduledTasks();
      
    final long now = System.currentTimeMillis();
    
    final List<SingularityPendingTaskId> dueTaskIds = Lists.newArrayListWithCapacity(tasks.size());
    
    for (SingularityPendingTaskId task : tasks) {
      if (task.getNextRunAt() <= now) {
        dueTaskIds.add(task);
      }
    }
    
    final List<SingularityTaskRequest> dueTasks = requestManager.fetchTasks(dueTaskIds);
    Collections.sort(dueTasks);
  
    return dueTasks;
  }

  public List<SingularityPendingTaskId> scheduleTasks(final List<SingularityPendingTaskId> scheduledTaskIds, final List<SingularityTaskId> activeTaskIds, SingularityRequest request) {
    for (SingularityPendingTaskId taskId : scheduledTaskIds) {
      if (taskId.getName().equals(request.getName())) {
        taskManager.deleteScheduledTask(taskId.toString());
      }
    }
    
    final List<SingularityPendingTaskId> scheduledTasks = getScheduledTaskIds(activeTaskIds, request);
    
    taskManager.persistScheduleTasks(scheduledTasks);
  
    return scheduledTasks;
  }
  
  public void scheduleOnCompletion(String stringTaskId) {
    SingularityTaskId taskId = SingularityTaskId.fromString(stringTaskId);
    
    Optional<SingularityRequest> maybeRequest = requestManager.fetchRequest(taskId.getName());
    
    if (!maybeRequest.isPresent()) {
      // TODO what about failures?
      LOG.warn(String.format("Not scheduling a new task, due to no existing request for %s", taskId.getName()));
      return;
    }
    
    SingularityRequest request = maybeRequest.get();
    
    final List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIds();
        
    taskManager.persistScheduleTasks(getScheduledTaskIds(activeTaskIds, request));
  }
  
  private List<SingularityPendingTaskId> getScheduledTaskIds(List<SingularityTaskId> activeTaskIds, SingularityRequest request) {
    final int numInstances = Objects.firstNonNull(request.getInstances(), 1);
    
    long nextRunAt = System.currentTimeMillis();
    
    if (request.getSchedule() != null) {
      try {
        final Date now = new Date();
        
        CronExpression cronExpression = new CronExpression(request.getSchedule());

        final Date nextRunAtDate = cronExpression.getNextValidTimeAfter(now);
        nextRunAt = nextRunAtDate.getTime();
        
        LOG.trace(String.format("Scheduling next run of %s (schedule: %s) at %s (now: %s)", request.getName(), request.getSchedule(), nextRunAtDate, now));
      } catch (ParseException pe) {
        throw Throwables.propagate(pe);
      }
    }
  
    int highestInstanceNo = 0;
    
    final List<SingularityTaskId> matchingTaskIds = SingularityTaskId.filter(activeTaskIds, request.getName());
    
    final int numMissingInstances = numInstances - matchingTaskIds.size();
    
    if (numMissingInstances > 0) {
      for (SingularityTaskId matchingTaskId : matchingTaskIds) {
        if (matchingTaskId.getInstanceNo() > highestInstanceNo) {
          highestInstanceNo = matchingTaskId.getInstanceNo();
        }
      }
    }
    
    // TODO handle this? it should never happen. We could scale down / kill the tasks.
    if (numMissingInstances < 0) {
      LOG.error(String.format("Missing instances is negative: %s, request %s, matching tasks: %s", numMissingInstances, request, matchingTaskIds));
      return Collections.emptyList();
    }
    
    final List<SingularityPendingTaskId> newTaskIds = Lists.newArrayListWithCapacity(numMissingInstances);
    
    for (int i = 0; i < numMissingInstances; i++) {
      newTaskIds.add(new SingularityPendingTaskId(request.getName(), nextRunAt, i + 1 + highestInstanceNo));
    }
    
    return newTaskIds;
  }
  
}
