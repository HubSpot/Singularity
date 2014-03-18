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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.singularity.SingularityDeployMarker;
import com.hubspot.singularity.SingularityDeployState;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRack;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestCleanup;
import com.hubspot.singularity.SingularityRequestCleanup.RequestCleanupType;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskCleanup.TaskCleanupType;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RackManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.TaskRequestManager;
import com.hubspot.singularity.data.history.HistoryManager;
import com.hubspot.singularity.data.history.HistoryManager.OrderDirection;
import com.hubspot.singularity.data.history.HistoryManager.RequestHistoryOrderBy;
import com.hubspot.singularity.data.history.HistoryManager.TaskHistoryOrderBy;
import com.hubspot.singularity.smtp.SingularityMailer;

public class SingularityScheduler {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityScheduler.class);
  
  private final TaskManager taskManager;
  private final RequestManager requestManager;
  private final TaskRequestManager taskRequestManager;
  private final DeployManager deployManager;
  
  private final SlaveManager slaveManager;
  private final RackManager rackManager;
  
  private final HistoryManager historyManager;
  private final SingularityMailer mailer;
  
  @Inject
  public SingularityScheduler(TaskRequestManager taskRequestManager, DeployManager deployManager, TaskManager taskManager, RequestManager requestManager, SlaveManager slaveManager, RackManager rackManager, HistoryManager historyManager, SingularityMailer mailer) {
    this.taskRequestManager = taskRequestManager;
    this.deployManager = deployManager;
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
      
      taskManager.createCleanupTask(new SingularityTaskCleanup(Optional.<String> absent(), TaskCleanupType.DECOMISSIONING, System.currentTimeMillis(), task.getTaskId()));
    } else {
      LOG.trace(String.format("Not adding scheduled task %s to cleanup queue", task.getTaskId()));
    }
  }
  
  public void checkForDecomissions(SingularitySchedulerStateCache stateCache) {
    final long start = System.currentTimeMillis();
    
    final Set<String> requestIdsToReschedule = Sets.newHashSet();
    final Set<SingularityTaskId> matchingTaskIds = Sets.newHashSet();
    
    final List<SingularityTaskId> activeTaskIds = stateCache.getActiveTaskIds();
    
    final List<SingularitySlave> slaves = slaveManager.getDecomissioningObjectsFiltered(stateCache.getDecomissioningSlaves());
    
    for (SingularitySlave slave : slaves) {
      for (SingularityTask activeTask : taskManager.getTasksOnSlave(activeTaskIds, slave)) {
        checkTaskForDecomissionCleanup(requestIdsToReschedule, matchingTaskIds, activeTask, slave.toString());
      }
    }
    
    final List<SingularityRack> racks = rackManager.getDecomissioningObjectsFiltered(stateCache.getDecomissioningRacks());
    
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
      requestManager.addToPendingQueue(new SingularityPendingRequest(requestId, Optional.<String> absent(), PendingType.DECOMISSIONED_SLAVE_OR_RACK));
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
  
  public void drainPendingQueue(final SingularitySchedulerStateCache stateCache) {
    final long start = System.currentTimeMillis();
    
    final List<SingularityPendingRequest> pendingRequests = requestManager.getPendingRequests();
    
    LOG.info(String.format("Pending queue had %s requests", pendingRequests.size()));
    
    if (pendingRequests.isEmpty()) {
      return;
    }
    
    int totalNewScheduledTasks = 0;
    int obsoleteRequests = 0;
    
    for (SingularityPendingRequest pendingRequest : pendingRequests) {
      Optional<SingularityRequest> maybeRequest = requestManager.fetchRequest(pendingRequest.getRequestId());
      
      if (maybeRequest.isPresent()) {
        checkForBounceAndAddToCleaningTasks(pendingRequest, stateCache.getActiveTaskIds(), stateCache.getCleaningTasks());
        
        int numScheduledTasks = scheduleTasks(stateCache, maybeRequest.get(), pendingRequest);
      
        LOG.debug(String.format("Pending request %s resulted in %s new scheduled tasks", pendingRequest, numScheduledTasks));
      
        totalNewScheduledTasks += numScheduledTasks;
      } else {
        LOG.debug(String.format("Pending request %s was obsolete (no matching active request)", pendingRequest));
        
        obsoleteRequests++;
      }
      
      requestManager.deletePendingRequest(pendingRequest);
    }
    
    LOG.info(String.format("Scheduled %s new tasks (%s obsolete requests) in %sms", totalNewScheduledTasks, obsoleteRequests, System.currentTimeMillis() - start));
  }
  
  private void checkForBounceAndAddToCleaningTasks(SingularityPendingRequest pendingRequest, final List<SingularityTaskId> activeTaskIds, final List<SingularityTaskId> cleaningTasks) {
    if (pendingRequest.getPendingTypeEnum() != PendingType.BOUNCE) {
      return;
    }
    
    final long now = System.currentTimeMillis(); 
    
    // TODO bounce MUST have a depoy ID!
    final List<SingularityTaskId> matchingTaskIds = SingularityTaskId.matchingAndNotIn(activeTaskIds, pendingRequest.getRequestId(), pendingRequest.getDeployId().get(),cleaningTasks);
    
    for (SingularityTaskId matchingTaskId : matchingTaskIds) {
      LOG.debug(String.format("Adding task %s to cleanup (bounce)", matchingTaskId.getId()));
      
      taskManager.createCleanupTask(new SingularityTaskCleanup(pendingRequest.getUser(), TaskCleanupType.BOUNCING, now, matchingTaskId));
      cleaningTasks.add(matchingTaskId);
    }
    
    LOG.info(String.format("Added %s tasks for request %s to cleanup bounce queue in %sms", matchingTaskIds.size(), pendingRequest.getRequestId(), System.currentTimeMillis() - now));
  }
    
  public List<SingularityTaskRequest> getDueTasks() {
    final List<SingularityPendingTask> tasks = taskManager.getScheduledTasks();
      
    final long now = System.currentTimeMillis();
    
    final List<SingularityPendingTask> dueTasks = Lists.newArrayListWithCapacity(tasks.size());
    
    for (SingularityPendingTask task : tasks) {
      if (task.getPendingTaskId().getNextRunAt() <= now) {
        dueTasks.add(task);
      }
    }
    
    final List<SingularityTaskRequest> dueTaskRequests = taskRequestManager.getTaskRequests(dueTasks);
    Collections.sort(dueTaskRequests);
    
    checkForStaleScheduledTasks(dueTasks, dueTaskRequests);
    
    return dueTaskRequests;
  }
  
  private void checkForStaleScheduledTasks(List<SingularityPendingTask> pendingTasks, List<SingularityTaskRequest> taskRequests) {
    final Set<String> foundRequestIds = Sets.newHashSetWithExpectedSize(taskRequests.size());
    for (SingularityTaskRequest taskRequest : taskRequests) {
      foundRequestIds.add(taskRequest.getRequest().getId());
    }
    for (SingularityPendingTask pendingTask : pendingTasks) {
      if (!foundRequestIds.contains(pendingTask.getPendingTaskId().getRequestId())) {
        LOG.info(String.format("Removing stale pending task %s because there was no found request id", pendingTask.getPendingTaskId()));
        taskManager.deleteScheduledTask(pendingTask.getPendingTaskId().getId());
      }
    }
  }
  
  private void deleteScheduledTasks(final List<SingularityPendingTask> scheduledTasks, String requestId) {
    for (SingularityPendingTask task : Iterables.filter(scheduledTasks, SingularityPendingTask.matching(requestId))) {
      taskManager.deleteScheduledTask(task.getPendingTaskId().getId());
    }
  }

  private Optional<String> getDeployId(SingularityPendingRequest pendingRequest) {
    if (pendingRequest.getDeployId().isPresent()) {
      return pendingRequest.getDeployId();
    }
    
    Optional<SingularityDeployState> deployState = deployManager.getDeployState(pendingRequest.getRequestId());
    
    if (!deployState.isPresent()) {
      LOG.warn(String.format("No deploy state found for pending request %s", pendingRequest));
      return Optional.absent();
    }
    
    Optional<SingularityDeployMarker> activeDeployMarker = deployState.get().getActiveDeploy();
    
    if (!activeDeployMarker.isPresent()) {
      LOG.warn(String.format("No active deploy found for pending request %s", pendingRequest));
      return Optional.absent();
    }
    
    return Optional.of(activeDeployMarker.get().getDeployId());
  }
  
  private int scheduleTasks(SingularitySchedulerStateCache stateCache, SingularityRequest request, SingularityPendingRequest pendingRequest) {
    deleteScheduledTasks(stateCache.getScheduledTasks(), request.getId());
    
    Optional<String> deployId = getDeployId(pendingRequest);
    
    if (!deployId.isPresent()) {
      LOG.warn(String.format("No deployId for pending request %s, not scheduling tasks", pendingRequest));
      return 0;
    }
    
    final List<SingularityTaskId> matchingTaskIds = SingularityTaskId.matchingAndNotIn(stateCache.getActiveTaskIds(), request.getId(), deployId.get(), stateCache.getCleaningTasks());
    
    final int numMissingInstances = getNumMissingInstances(matchingTaskIds, request);

    if (numMissingInstances > 0) {
      LOG.debug(String.format("Missing %s instances of request %s (matching tasks: %s), pending request: %s", numMissingInstances, request.getId(), matchingTaskIds, pendingRequest));
      
      final List<SingularityPendingTask> scheduledTasks = getScheduledTaskIds(numMissingInstances, matchingTaskIds, request, deployId.get(), pendingRequest);
      
      LOG.trace(String.format("Scheduling tasks: %s", scheduledTasks));
      
      taskManager.persistScheduleTasks(scheduledTasks);
    } else if (numMissingInstances < 0) {
      LOG.debug(String.format("Missing instances is negative: %s, request %s, matching tasks: %s", numMissingInstances, request, matchingTaskIds));
      
      final long now = System.currentTimeMillis();
      
      for (int i = 0; i < Math.abs(numMissingInstances); i++) {
        final SingularityTaskId toCleanup = matchingTaskIds.get(i);
        
        LOG.info(String.format("Cleaning up task %s due to new request %s - scaling down to %s instances", toCleanup.getId(), request.getId(), request.getInstances()));
    
        taskManager.createCleanupTask(new SingularityTaskCleanup(Optional.<String> absent(), TaskCleanupType.SCALING_DOWN, now, toCleanup));
      }
    }
    
    return numMissingInstances;
  }
  
  private boolean wasDecomissioning(SingularityTaskId taskId, Optional<SingularityTask> maybeActiveTask, SingularitySchedulerStateCache stateCache) {
    if (!maybeActiveTask.isPresent()) {
      return false;
    }
    
    return stateCache.isSlaveDecomissioning(maybeActiveTask.get().getMesosTask().getSlaveId().getValue()) || stateCache.isRackDecomissioning(taskId.getRackId());
  }
  
  public void handleCompletedTask(Optional<SingularityTask> maybeActiveTask, String stringTaskId, TaskState state, SingularitySchedulerStateCache stateCache) {
    SingularityTaskId taskId = SingularityTaskId.fromString(stringTaskId);
 
    Optional<SingularityRequest> maybeRequest = requestManager.fetchRequest(taskId.getRequestId());
    
    if (!maybeRequest.isPresent()) {
      LOG.warn(String.format("Not scheduling a new task, due to no existing request for %s", taskId.getRequestId()));
      return;
    }
    
    final SingularityRequest request = maybeRequest.get();
    
    PendingType pendingType = PendingType.TASK_DONE;
    
    if (MesosUtils.isTaskFailed(state)) {
      if (!wasDecomissioning(taskId, maybeActiveTask, stateCache)) {
        mailer.sendTaskFailedMail(taskId, request, state);
      } else {
        LOG.debug(String.format("Not sending a task failure email because task %s was on a decomissioning slave/rack", taskId));
      }
      
      // TODO how to handle this if there are running tasks that are working?
      if (shouldPause(request)) {
        mailer.sendRequestPausedMail(taskId, request);
        requestManager.createCleanupRequest(new SingularityRequestCleanup(Optional.<String> absent(), RequestCleanupType.PAUSING, System.currentTimeMillis(), request.getId()));
        return;
      }
      
      if (request.isScheduled() && shouldRetryImmediately(request)) {
        pendingType = PendingType.RETRY;
      }
    }
    
    if (!request.isOneOff()) {
      scheduleTasks(stateCache, request, new SingularityPendingRequest(request.getId(), Optional.of(taskId.getDeployId()), pendingType));
    }
  }
  
  private boolean shouldRetryImmediately(SingularityRequest request) {
    if (!request.getNumRetriesOnFailure().isPresent()) {
      return false;
    }
   
    final int numRetriesInARow = getNumRetriesInARow(request);
    
    if (numRetriesInARow >= request.getNumRetriesOnFailure().get()) {
      LOG.debug(String.format("Request %s had %s retries in a row, not retrying again (num retries on failure: %s)", request.getId(), numRetriesInARow, request.getNumRetriesOnFailure()));
      return false;
    }
    
    LOG.debug(String.format("Request %s had %s retries in a row - retrying again (num retries on failure: %s)", request.getId(), numRetriesInARow, request.getNumRetriesOnFailure()));
    
    return true;
  } 
  
  private int getNumRetriesInARow(SingularityRequest request) {
    int retries = 0;
    
    List<SingularityTaskIdHistory> taskHistory = historyManager.getTaskHistoryForRequest(request.getId(), Optional.of(TaskHistoryOrderBy.createdAt), Optional.of(OrderDirection.DESC), 0, request.getNumRetriesOnFailure().get());
    
    for (SingularityTaskIdHistory history : taskHistory) { 
      if (history.getLastStatus().isPresent()) {
        TaskState taskState = TaskState.valueOf(history.getLastStatus().get());
        
        if (MesosUtils.isTaskFailed(taskState) && PendingType.valueOf(history.getPendingType()) == PendingType.RETRY) {
          retries++;
        } else {
          break;
        }
      }
    }
    
    return retries;
  }
  
  private boolean shouldPause(SingularityRequest request) {
    if (request.getPauseOnInitialFailure().or(Boolean.FALSE)) {
      if (historyManager.getTaskHistoryForRequest(request.getId(), Optional.of(TaskHistoryOrderBy.createdAt), Optional.of(OrderDirection.DESC), 0, 1).isEmpty()) {
        LOG.info(String.format("Pausing request %s due to initial failure", request.getId()));
        return true;
      }
    }

    if (!request.getMaxFailuresBeforePausing().isPresent()) {
      return false;
    }
    
    final int pauseAfterNumFailedTasks = request.getMaxFailuresBeforePausing().get() + 1;
    
    if (request.getMaxFailuresBeforePausing().get() > 0) {
      SingularityRequestHistory lastUpdate = Iterables.getFirst(historyManager.getRequestHistory(request.getId(), Optional.of(RequestHistoryOrderBy.createdAt), Optional.of(OrderDirection.DESC), 0, 1), null);
      long lastUpdateAt = 0;
      
      if (lastUpdate != null) {
        lastUpdateAt = lastUpdate.getCreatedAt();
      } else {
        LOG.warn(String.format("Couldn't find a historical request for %s", request.getId()));
      }
            
      List<SingularityTaskIdHistory> taskHistory = historyManager.getTaskHistoryForRequest(request.getId(), Optional.of(TaskHistoryOrderBy.createdAt), Optional.of(OrderDirection.DESC), 0, pauseAfterNumFailedTasks);
      
      LOG.trace(String.format("Found %s historical tasks for request %s", taskHistory.size(), request.getId()));
      
      if (taskHistory.size() < pauseAfterNumFailedTasks) {
        return false;
      }
      
      for (SingularityTaskIdHistory history : taskHistory) {
        if (lastUpdateAt > history.getCreatedAt()) {
          LOG.debug(String.format("Not pausing request %s because task %s was launched at %s, before the last request update (%s)", request.getId(), history.getTaskId(), history.getCreatedAt(), lastUpdateAt));
          return false;
        }
        
        if (history.getLastStatus().isPresent()) {
          TaskState taskState = TaskState.valueOf(history.getLastStatus().get());
          
          if (!MesosUtils.isTaskFailed(taskState)) {
            LOG.debug(String.format("Task %s was not a failure (%s), so request %s is not paused", history.getTaskId(), taskState, request.getId())); 
                
            return false;
          }
        }
      }
    }
  
    LOG.info(String.format("Pausing request %s because it has failed at least %s tasks in a row", request.getId(), pauseAfterNumFailedTasks));
    
    return true;
  }
   
  private final int getNumMissingInstances(List<SingularityTaskId> matchingTaskIds, SingularityRequest request) {
    final int numInstances = request.getInstances().or(1);
    
    return numInstances - matchingTaskIds.size();
  }
  
  private List<SingularityPendingTask> getScheduledTaskIds(int numMissingInstances, List<SingularityTaskId> matchingTaskIds, SingularityRequest request, String deployId, SingularityPendingRequest pendingRequest) {
    final long nextRunAt = getNextRunAt(request, pendingRequest.getPendingTypeEnum());
  
    int highestInstanceNo = 0;

    for (SingularityTaskId matchingTaskId : matchingTaskIds) {
      if (matchingTaskId.getInstanceNo() > highestInstanceNo) {
        highestInstanceNo = matchingTaskId.getInstanceNo();
      }
    }
    
    final List<SingularityPendingTask> newTasks = Lists.newArrayListWithCapacity(numMissingInstances);
    
    for (int i = 0; i < numMissingInstances; i++) {
      newTasks.add(new SingularityPendingTask(new SingularityPendingTaskId(request.getId(), deployId, nextRunAt, i + 1 + highestInstanceNo, pendingRequest.getPendingTypeEnum()), pendingRequest.getCmdLineArgs()));
    }
    
    return newTasks;
  }
  
  private long getNextRunAt(SingularityRequest request, PendingType pendingType) {
    final long now = System.currentTimeMillis();
    
    long nextRunAt = now;
    
    if (!request.isScheduled()) {
      return nextRunAt;
    }
    
    if (pendingType == PendingType.IMMEDIATE || pendingType == PendingType.RETRY) {
      LOG.info("Scheduling requested immediate run of %s", request.getId());
    } else {
      try {
        Date scheduleFrom = new Date(now);
        
        // find out what the last time the task ran at was.
        Optional<SingularityTaskIdHistory> history = historyManager.getLastTaskForRequest(request.getId());
        if (history.isPresent()) {
          scheduleFrom = new Date(history.get().getCreatedAt());
        } else {
          // if the request has never ran before, schedule from its request creation.
          SingularityRequestHistory requestHistory = Iterables.getFirst(historyManager.getRequestHistory(request.getId(), Optional.of(RequestHistoryOrderBy.createdAt), Optional.of(OrderDirection.ASC), 0, 1), null);
          
          if (requestHistory != null) {
            scheduleFrom = new Date(requestHistory.getCreatedAt());
          } else {
            LOG.warn(String.format("Could not find request history for request %s - task will be scheduled based on %s", request.getId(), now));
          }
        }
        
        CronExpression cronExpression = new CronExpression(request.getSchedule().get());

        final Date nextRunAtDate = cronExpression.getNextValidTimeAfter(scheduleFrom);

        LOG.trace(String.format("Calculating nextRunAtDate for %s (schedule: %s): %s (from: %s)", request.getId(), request.getSchedule(), nextRunAtDate, scheduleFrom));

        nextRunAt = Math.max(nextRunAtDate.getTime(), now); // don't create a schedule that is overdue as this is used to indicate that singularity is not fulfilling requests.
        
        LOG.trace(String.format("Scheduling next run of %s (schedule: %s) at %s (from: %s)", request.getId(), request.getSchedule(), nextRunAtDate, scheduleFrom));
      } catch (ParseException pe) {
        throw Throwables.propagate(pe);
      }
    }
    
    return nextRunAt;
  }
}
