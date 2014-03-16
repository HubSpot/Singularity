package com.hubspot.singularity.scheduler;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.mesos.Protos.TaskState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.singularity.SingularityDriverManager;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.history.HistoryManager;
import com.hubspot.singularity.smtp.SingularityMailer;

public class SingularityStaleTaskChecker {
  
  private final static Logger LOG = LoggerFactory.getLogger(SingularityStaleTaskChecker.class);

  private final SingularityConfiguration configuration;
  private final TaskManager taskManager;
  private final RequestManager requestManager;
  private final SingularityMailer mailer;
  private final SingularityDriverManager driverManager;
  private final HistoryManager historyManager;
  
  @Inject
  public SingularityStaleTaskChecker(SingularityConfiguration configuration, RequestManager requestManager, TaskManager taskManager, SingularityMailer mailer, HistoryManager historyManager, SingularityDriverManager driverManager) {
    this.configuration = configuration;
    this.requestManager = requestManager;
    this.taskManager = taskManager;
    this.mailer = mailer;
    this.historyManager = historyManager;
    this.driverManager = driverManager;
  }
  
  private boolean isTaskOldEnoughToIgnore(final Optional<Long> previousTimestamp, final long startedAt) {
    if (!previousTimestamp.isPresent()) {
      return false;
    }
    
    long taskDurationAsOfLastCheck = previousTimestamp.get() - startedAt;
    
    return taskDurationAsOfLastCheck > getAsMillis(configuration.getKillAfterTasksDoNotRunDefaultSeconds());
  }
  
  private boolean isTaskRunning(SingularityTaskId taskId) {
    Optional<SingularityTaskHistory> taskHistory = historyManager.getTaskHistory(taskId.getId(), true);
    
    if (!taskHistory.isPresent()) {
      LOG.warn(String.format("Expected to find task history for task %s, but it was absent", taskId));
      return false;
    }
    
    boolean wasRunning = false;
    
    for (SingularityTaskHistoryUpdate update : taskHistory.get().getTaskUpdates()) {
      TaskState state = TaskState.valueOf(update.getStatusUpdate());
      
      if (MesosUtils.isTaskDone(state)) {
        LOG.warn(String.format("Expected to find an active task for %s, but instead found state: %s", taskId, update.getStatusUpdate()));
        return false;
      }
      
      if (state == TaskState.TASK_RUNNING) {
        wasRunning = true;
      }
    }
    
    return wasRunning;
  }
  
  public int checkForStaleTasks(final Optional<Long> previousTimestamp, final long currentTimestamp) {
    int numStaleTasks = 0;
    
    final List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIds();
    final Set<String> requestIds = Sets.newHashSet(requestManager.getRequestIds());
    
    for (SingularityTaskId taskId : activeTaskIds) {
      final long startedAt = taskId.getStartedAt();
      
      if (!requestIds.contains(taskId.getRequestId())) {
        LOG.warn(String.format("Killing a task %s which had no matching request", taskId));
        numStaleTasks++;
        driverManager.kill(taskId.getId());
        continue;
      }
      
      // was this task already sufficiently old at last check time? 
      if (isTaskOldEnoughToIgnore(previousTimestamp, startedAt)) {
        LOG.trace(String.format("Not checking a task %s for staleness because it was old enough (%s) as of (%s)", taskId, startedAt, previousTimestamp));
        continue;
      }
      
      // how long has this task been running? 
      
      final long taskDuration = currentTimestamp - startedAt;
      
      LOG.trace(String.format("Checking task %s for staleness, duration %s", taskId, DurationFormatUtils.formatDurationHMS(taskDuration)));
      
      if (taskDuration > getAsMillis(configuration.getKillAfterTasksDoNotRunDefaultSeconds())) {
        // check and kill the task
        if (!isTaskRunning(taskId)) {
          LOG.info(String.format("Killing a task %s which is not running after %s", taskId, taskDuration));
          numStaleTasks++;
          driverManager.kill(taskId.getId());
        }
        
      } else if (taskDuration > getAsMillis(configuration.getWarnAfterTasksDoNotRunDefaultSeconds())) {
        
        // check and warn about the task - only if previously we would not have warned about it
        final long taskDurationAsOfLastCheck = previousTimestamp.isPresent() ? previousTimestamp.get() - startedAt : 0; 
        
        if (taskDurationAsOfLastCheck < getAsMillis(configuration.getWarnAfterTasksDoNotRunDefaultSeconds())) {
          if (!isTaskRunning(taskId)) {
            LOG.info(String.format("Found a possible stale task %s - sending a warning (duration: %s)", taskId, DurationFormatUtils.formatDurationHMS(taskDuration)));
            
            Optional<SingularityRequest> maybeRequest = requestManager.fetchRequest(taskId.getRequestId());
            if (maybeRequest.isPresent()) {
              mailer.sendTaskNotRunningWarningEmail(taskId, taskDuration, maybeRequest.get());
            } else {
              LOG.warn(String.format("Didn't find a request for task %s", taskId));
            }  
          }
        }
      }
    }
    
    return numStaleTasks;
  }

  private long getAsMillis(long seconds) {
    return TimeUnit.SECONDS.toMillis(seconds);
  }
  
}
