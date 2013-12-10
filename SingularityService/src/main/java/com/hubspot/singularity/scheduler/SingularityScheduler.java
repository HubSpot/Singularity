package com.hubspot.singularity.scheduler;

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.mesos.Protos.TaskState;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.singularity.SingularityPendingRequestId;
import com.hubspot.singularity.SingularityPendingRequestId.PendingType;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRack;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskCleanup.CleanupType;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.data.RackManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.history.HistoryManager;
import com.hubspot.singularity.data.history.HistoryManager.OrderDirection;
import com.hubspot.singularity.data.history.HistoryManager.TaskHistoryOrderBy;
import com.hubspot.singularity.smtp.SingularityMailer;

public class SingularityScheduler extends SingularitySchedulerBase {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityScheduler.class);
  
  private final TaskManager taskManager;
  private final RequestManager requestManager;
  
  private final SlaveManager slaveManager;
  private final RackManager rackManager;
  
  private final HistoryManager historyManager;
  private final SingularityMailer mailer;
  
  @Inject
  public SingularityScheduler(TaskManager taskManager, RequestManager requestManager, SlaveManager slaveManager, RackManager rackManager, HistoryManager historyManager, SingularityMailer mailer) {
    super(taskManager);
    this.taskManager = taskManager;
    this.requestManager = requestManager;
    this.slaveManager = slaveManager;
    this.rackManager = rackManager;
    this.historyManager = historyManager;
    this.mailer = mailer;
  }
  
  private void checkTaskForDecomissionCleanup(final Set<String> requestIdsToReschedule, final Set<SingularityTaskId> matchingTaskIds, SingularityTask task, String decomissioningObject) {
    requestIdsToReschedule.add(task.getTaskRequest().getRequest().getId());
    
    matchingTaskIds.add(task.getTaskId());

    if (!task.getTaskRequest().getRequest().isScheduled()) {
      LOG.trace(String.format("Scheduling a cleanup task for %s due to decomissioning %s", task.getTaskId(), decomissioningObject));
      
      taskManager.createCleanupTask(new SingularityTaskCleanup(Optional.<String> absent(), CleanupType.DECOMISSIONING, System.currentTimeMillis(), task.getTaskId().getId(), task.getTaskRequest().getRequest().getId()));
    } else {
      LOG.trace(String.format("Not adding scheduled task %s to cleanup queue", task.getTaskId()));
    }
  }
  
  public void checkForDecomissions(List<SingularityTaskId> activeTaskIds) {
    final long start = System.currentTimeMillis();
    
    final Set<String> requestIdsToReschedule = Sets.newHashSet();
    final Set<SingularityTaskId> matchingTaskIds = Sets.newHashSet();
    
    final List<SingularitySlave> slaves = slaveManager.getDecomissioningObjectsFiltered();
    
    for (SingularitySlave slave : slaves) {
      for (SingularityTaskId activeTaskId : activeTaskIds) {
        if (activeTaskId.getHost().equals(slave.getHost())) {
          Optional<SingularityTask> maybeTask = taskManager.getActiveTask(activeTaskId.getId());
          if (slave.getId().equals(maybeTask.get().getOffer().getSlaveId().getValue())) {
            checkTaskForDecomissionCleanup(requestIdsToReschedule, matchingTaskIds, maybeTask.get(), slave.toString());
          }
        }
      }
    }
    
    final List<SingularityRack> racks = rackManager.getDecomissioningObjectsFiltered();
    
    for (SingularityRack rack : racks) {
      for (SingularityTaskId activeTaskId : activeTaskIds) {
        if (matchingTaskIds.contains(activeTaskId)) {
          continue;
        }
    
        if (rack.getId().equals(activeTaskId.getRackId())) {
          Optional<SingularityTask> maybeTask = taskManager.getActiveTask(activeTaskId.getId());
          checkTaskForDecomissionCleanup(requestIdsToReschedule, matchingTaskIds, maybeTask.get(), rack.toString());
        }
      }
    }
    
    for (String requestId : requestIdsToReschedule) {
      LOG.trace(String.format("Rescheduling request %s due to decomissions", requestId));
      requestManager.addToPendingQueue(new SingularityPendingRequestId(requestId));
    }
    
    for (SingularitySlave slave : slaves) {
      LOG.debug(String.format("Marking slave %s as decomissioned", slave));
      slaveManager.markAsDecomissioned(slave);
    }
    
    for (SingularityRack rack : racks) {
      LOG.debug(String.format("Marking rack %s as decomissioned", rack));
      rackManager.markAsDecomissioned(rack);
    }

    LOG.info(String.format("Found %s decomissioning slaves, %s decomissioning racks, rescheduling %s requests and scheduling %s tasks for cleanup in %sms", slaves.size(), racks.size(), requestIdsToReschedule.size(), matchingTaskIds.size(), System.currentTimeMillis() - start));
  }
  
  public void drainPendingQueue(final List<SingularityTaskId> activeTaskIds) {
    final long start = System.currentTimeMillis();
    
    final List<SingularityPendingRequestId> pendingRequests = requestManager.getPendingRequestIds();
    
    LOG.info(String.format("Pending queue had %s requests", pendingRequests.size()));
    
    if (pendingRequests.isEmpty()) {
      return;
    }
    
    final List<SingularityPendingTaskId> scheduledTasks = taskManager.getScheduledTasks();
    final List<String> decomissioningRacks = rackManager.getDecomissioning();
    final List<SingularitySlave> decomissioningSlaves = slaveManager.getDecomissioningObjects();
    
    int numScheduledTasks = 0;
    int obsoleteRequests = 0;
    
    for (SingularityPendingRequestId pendingRequest : pendingRequests) {
      Optional<SingularityRequest> maybeRequest = requestManager.fetchRequest(pendingRequest.getRequestId());
      
      if (maybeRequest.isPresent()) {
        numScheduledTasks += scheduleTasks(scheduledTasks, activeTaskIds, decomissioningRacks, decomissioningSlaves, maybeRequest.get(), pendingRequest.getPendingTypeEnum()).size();
      } else {
        obsoleteRequests++;
      }
      
      requestManager.deletePendingRequest(pendingRequest.toString());
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
  
  private void deleteScheduledTasks(final List<SingularityPendingTaskId> scheduledTaskIds, String requestId) {
    for (SingularityPendingTaskId taskId : SingularityPendingTaskId.filter(scheduledTaskIds, requestId)) {
      taskManager.deleteScheduledTask(taskId.toString());
    }
  }

  public List<SingularityPendingTaskId> scheduleTasks(final List<SingularityPendingTaskId> scheduledTaskIds, final List<SingularityTaskId> activeTaskIds, List<String> decomissioningRacks, List<SingularitySlave> decomissioningSlaves, SingularityRequest request, PendingType pendingType) {
    deleteScheduledTasks(scheduledTaskIds, request.getId());
    
    final List<SingularityPendingTaskId> scheduledTasks = getScheduledTaskIds(activeTaskIds, decomissioningRacks, decomissioningSlaves, request, pendingType);
    
    taskManager.persistScheduleTasks(scheduledTasks);
  
    return scheduledTasks;
  }
  
  public void handleCompletedTask(String stringTaskId, TaskState state) {
    SingularityTaskId taskId = SingularityTaskId.fromString(stringTaskId);
 
    Optional<SingularityRequest> maybeRequest = requestManager.fetchRequest(taskId.getRequestId());
    
    if (!maybeRequest.isPresent()) {
      // TODO what about failures?
      LOG.warn(String.format("Not scheduling a new task, due to no existing request for %s", taskId.getRequestId()));
      return;
    }
    
    SingularityRequest request = maybeRequest.get();
    
    if (MesosUtils.isTaskFailed(state)) {
      // TODO send an email every time?
      mailer.sendTaskFailedMail(taskId, request, state);
      
      if (shouldPause(request)) {
        requestManager.pause(request);
        return;
      }
    }
    
    scheduleOnCompletion(maybeRequest.get());
  }
  
  private boolean shouldPause(SingularityRequest request) {
    if (request.getNumRetriesOnFailure() != null) {
      return false;
    }
    
    List<SingularityTaskIdHistory> taskHistory = historyManager.getTaskHistoryForRequest(request.getId(), Optional.of(TaskHistoryOrderBy.createdAt), Optional.of(OrderDirection.DESC), 0, request.getNumRetriesOnFailure());
    
    if (taskHistory.size() < request.getNumRetriesOnFailure()) {
      return false;
    }
    
    for (SingularityTaskIdHistory history : taskHistory) {
      if (history.getLastStatus().isPresent()) {
        TaskState taskState = TaskState.valueOf(history.getLastStatus().get());
        
        if (!MesosUtils.isTaskFailed(taskState)) {
          return false;
        }
      }
    }
  
    return true;
  }
   
  public void scheduleOnCompletion(SingularityRequest request) {
    final List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIds();
    final List<SingularityPendingTaskId> scheduledTasks = taskManager.getScheduledTasks();
    final List<String> decomissioningRacks = rackManager.getDecomissioning();
    final List<SingularitySlave> decomissioningSlaves = slaveManager.getDecomissioningObjects();
    
    scheduleTasks(scheduledTasks, activeTaskIds, decomissioningRacks, decomissioningSlaves, request, PendingType.REGULAR);
  }
  
  private List<SingularityPendingTaskId> getScheduledTaskIds(List<SingularityTaskId> activeTaskIds, List<String> decomissioningRacks, List<SingularitySlave> decomissioningSlaves, SingularityRequest request, PendingType pendingType) {
    final int numInstances = request.getInstances();
    
    final long nextRunAt = getNextRunAt(request, pendingType);
    
    int highestInstanceNo = 0;
    
    final List<SingularityTaskId> matchingTaskIds = getMatchingActiveTaskIds(request.getId(), activeTaskIds, decomissioningRacks, decomissioningSlaves);
    
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
      newTaskIds.add(new SingularityPendingTaskId(request.getId(), nextRunAt, i + 1 + highestInstanceNo));
    }
    
    return newTaskIds;
  }
  
  private long getNextRunAt(SingularityRequest request, PendingType pendingType) {
    long nextRunAt = System.currentTimeMillis();
    
    if (!request.isScheduled()) {
      return nextRunAt;
    }
    
    if (pendingType == PendingType.IMMEDIATE) {
      LOG.info("Scheduling requested immediate run of %s", request.getId());
    } else {
      try {
        Date scheduleFrom = new Date();
        
        if (pendingType == PendingType.STARTUP) {
          // find out what the last time the task ran at was.
          Optional<SingularityTaskIdHistory> history = historyManager.getLastTaskForRequest(request.getId());
          if (history.isPresent()) {
            scheduleFrom = new Date(history.get().getUpdatedAt().or(history.get().getCreatedAt()));
          }
        }
        
        CronExpression cronExpression = new CronExpression(request.getSchedule());

        final Date nextRunAtDate = cronExpression.getNextValidTimeAfter(scheduleFrom);
        nextRunAt = nextRunAtDate.getTime();
        
        LOG.trace(String.format("Scheduling next run of %s (schedule: %s) at %s (from: %s)", request.getId(), request.getSchedule(), nextRunAtDate, scheduleFrom));
      } catch (ParseException pe) {
        throw Throwables.propagate(pe);
      }
    }
    
    return nextRunAt;
  }
}
