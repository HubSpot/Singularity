package com.hubspot.singularity.scheduler;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

import org.apache.mesos.Protos.TaskState;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SingularityTaskId;
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
  
  public List<SingularityTaskId> scheduleTasks(SingularityRequest request) {
    final List<SingularityTaskId> scheduledTasks = getScheduledTaskIds(request);
    
    taskManager.persistScheduleTasks(scheduledTasks);
  
    return scheduledTasks;
  }
  
  public void scheduleOnCompletion(TaskState state, String stringTaskId) {
    SingularityTaskId taskId = SingularityTaskId.fromString(stringTaskId);
    
    Optional<SingularityRequest> maybeRequest = requestManager.fetchRequest(taskId.getName());
    
    if (!maybeRequest.isPresent()) {
      // TODO what about failures?
      LOG.warn(String.format("Not scheduling a new task, due to no existing request for %s", taskId.getName()));
      return;
    }
    
    SingularityRequest request = maybeRequest.get();
    
    if (request.alwaysRunning()) {
      int requiredInstances = request.getInstances();
      
      List<SingularityTaskId> allTaskIds = taskManager.getActiveTaskIds();
      List<SingularityTaskId> matchingTaskIds = Lists.newArrayListWithExpectedSize(requiredInstances);
      
      for (SingularityTaskId activeTaskId : allTaskIds) {
        if (activeTaskId.getName().equals(request.getName())) {
          matchingTaskIds.add(taskId);
        }
      }
      
      int numMissingInstances = requiredInstances - matchingTaskIds.size();
      
      if (numMissingInstances > 0) {
        final long now = System.currentTimeMillis();
        
        int highestInstanceNo = 0;
        
        for (SingularityTaskId matchingTaskId : matchingTaskIds) {
          if (matchingTaskId.getInstanceNo() > highestInstanceNo) {
            highestInstanceNo = matchingTaskId.getInstanceNo();
          }
        }
        
        final List<SingularityTaskId> newTaskIds = Lists.newArrayListWithCapacity(numMissingInstances);
        
        for (int i = 0; i < numMissingInstances; i++) {
          newTaskIds.add(new SingularityTaskId(request.getName(), now, i + 1 + highestInstanceNo));
        }
        
        taskManager.persistScheduleTasks(newTaskIds);
      }
    } else if (request.isScheduled()) {
      taskManager.persistScheduleTasks(getScheduledTaskIds(request));
    }
    
  }
  
  private List<SingularityTaskId> getScheduledTaskIds(SingularityRequest request) {
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
  
    final List<SingularityTaskId> taskIds = Lists.newArrayListWithCapacity(numInstances);

    for (int i = 0; i < numInstances; i++) {
      taskIds.add(new SingularityTaskId(request.getName(), nextRunAt, i));
    }
    
    return taskIds;
  }
  
}
