package com.hubspot.singularity.notifications;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Singleton;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.SingularityNotificationType;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.TaskCleanupType;

@Singleton
public class NotificationHelper {

  private static final Logger LOG = LoggerFactory.getLogger(NotificationHelper.class);
  private static final Pattern TASK_STATUS_BY_PATTERN = Pattern.compile("(\\w+) by \\w+");

  public Optional<SingularityNotificationType> getNotificationType(
      SingularityTaskHistory taskHistory,
      SingularityRequest request
  ) {
    Optional<SingularityTaskHistoryUpdate> lastUpdate = taskHistory.getLastTaskUpdate();
    if (!lastUpdate.isPresent()) {
      LOG.warn("Can't send task completed mail for task {} - no last update", taskHistory.getTask().getTaskId());
      return Optional.absent();
    }
    return getNotificationType(taskHistory.getLastTaskUpdate().get().getTaskState(), request, taskHistory.getTaskUpdates());
  }

  public Optional<SingularityNotificationType> getNotificationType(
      ExtendedTaskState taskState,
      SingularityRequest request,
      Collection<SingularityTaskHistoryUpdate> taskUpdates
  ) {
    Optional<SingularityTaskHistoryUpdate> cleaningUpdate =
        SingularityTaskHistoryUpdate.getUpdate(taskUpdates, ExtendedTaskState.TASK_CLEANING);

    switch (taskState) {
      case TASK_FAILED:
        if (cleaningUpdate.isPresent()) {
          Optional<TaskCleanupType> cleanupType = getCleanupType(cleaningUpdate.get());

          if (cleanupType.isPresent() && cleanupType.get() == TaskCleanupType.DECOMISSIONING) {
            return Optional.of(SingularityNotificationType.TASK_FAILED_DECOMISSIONED);
          }
        }
        return Optional.of(SingularityNotificationType.TASK_FAILED);
      case TASK_FINISHED:
        switch (request.getRequestType()) {
          case ON_DEMAND:
            return Optional.of(SingularityNotificationType.TASK_FINISHED_ON_DEMAND);
          case RUN_ONCE:
            return Optional.of(SingularityNotificationType.TASK_FINISHED_RUN_ONCE);
          case SCHEDULED:
            return Optional.of(SingularityNotificationType.TASK_FINISHED_SCHEDULED);
          case SERVICE:
          case WORKER:
            return Optional.of(SingularityNotificationType.TASK_FINISHED_LONG_RUNNING);
        }
      case TASK_KILLED:
        if (cleaningUpdate.isPresent()) {
          Optional<TaskCleanupType> cleanupType = getCleanupType(cleaningUpdate.get());

          if (cleanupType.isPresent()) {
            switch (cleanupType.get()) {
              case DECOMISSIONING:
                return Optional.of(SingularityNotificationType.TASK_KILLED_DECOMISSIONED);
              case UNHEALTHY_NEW_TASK:
              case OVERDUE_NEW_TASK:
                return Optional.of(SingularityNotificationType.TASK_KILLED_UNHEALTHY);
              default:
            }
          }
        }

        return Optional.of(SingularityNotificationType.TASK_KILLED);
      case TASK_LOST:
        return Optional.of(SingularityNotificationType.TASK_LOST);
      default:
        return Optional.absent();
    }
  }

  private static Optional<TaskCleanupType> getCleanupType(
      SingularityTaskHistoryUpdate taskHistoryUpdate
  ) {
    if (!taskHistoryUpdate.getStatusMessage().isPresent()) {
      return Optional.absent();
    }

    String taskCleanupTypeMsg = taskHistoryUpdate.getStatusMessage().get();

    Matcher matcher = TASK_STATUS_BY_PATTERN.matcher(taskCleanupTypeMsg);

    if (matcher.find()) {
      taskCleanupTypeMsg = matcher.group(1);
    }

    try {
      return Optional.of(TaskCleanupType.valueOf(taskCleanupTypeMsg.toUpperCase()));
    } catch (IllegalArgumentException iae) {
      LOG.warn("Couldn't parse TaskCleanupType from update {}", taskHistoryUpdate);
      return Optional.absent();
    }
  }


}
