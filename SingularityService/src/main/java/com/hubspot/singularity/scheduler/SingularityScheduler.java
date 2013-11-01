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
  
  @Inject
  public SingularityScheduler(TaskManager taskManager, RequestManager requestManager) {
    this.taskManager = taskManager;
    this.requestManager = requestManager;
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
    
    for (String pendingRequest : pendingRequests) {
      Optional<SingularityRequest> maybeRequest = requestManager.fetchRequest(pendingRequest);
      
      if (!maybeRequest.isPresent()) {
        continue;
      }
      
      numScheduledTasks += scheduleTasks(scheduledTasks, activeTaskIds, maybeRequest.get()).size();
      
      requestManager.deletePendingRequest(pendingRequest);
    }
    
    LOG.info(String.format("Scheduled %s requests in %sms", numScheduledTasks, System.currentTimeMillis() - start));
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
        CronExpression cronExpression = new CronExpression(request.getSchedule());
      
        nextRunAt = cronExpression.getNextValidTimeAfter(new Date()).getTime();
      } catch (ParseException pe) {
        throw Throwables.propagate(pe);
      }
    }
  
    int highestInstanceNo = 0;
    
    List<SingularityTaskId> matchingTaskIds = Lists.newArrayListWithExpectedSize(numInstances);
    
    for (SingularityTaskId activeTaskId : activeTaskIds) {
      if (activeTaskId.getName().equals(request.getName())) {
        matchingTaskIds.add(activeTaskId);
      }
    }
    
    final int numMissingInstances = numInstances - matchingTaskIds.size();
    
    if (numMissingInstances > 0) {
      for (SingularityTaskId matchingTaskId : matchingTaskIds) {
        if (matchingTaskId.getInstanceNo() > highestInstanceNo) {
          highestInstanceNo = matchingTaskId.getInstanceNo();
        }
      }
    }
    
    final List<SingularityPendingTaskId> newTaskIds = Lists.newArrayListWithCapacity(numMissingInstances);
    
    for (int i = 0; i < numMissingInstances; i++) {
      newTaskIds.add(new SingularityPendingTaskId(request.getName(), nextRunAt, i + 1 + highestInstanceNo));
    }
    
    return newTaskIds;
  }
  
}
