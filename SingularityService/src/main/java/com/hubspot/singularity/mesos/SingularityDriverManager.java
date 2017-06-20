package com.hubspot.singularity.mesos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.RequestCleanupType;
import com.hubspot.singularity.SingularityKilledTaskIdRecord;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskDestroyFrameworkMessage;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.transcoders.Transcoder;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

@Singleton
public class SingularityDriverManager {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityDriverManager.class);

  private final SingularityDriver singularityDriver;
  private final TaskManager taskManager;
  private final Transcoder<SingularityTaskDestroyFrameworkMessage> transcoder;
  private final SingularityExceptionNotifier exceptionNotifier;

  @Inject
  public SingularityDriverManager(SingularityDriver singularityDriver,
                                  TaskManager taskManager,
                                  Transcoder<SingularityTaskDestroyFrameworkMessage> transcoder,
                                  SingularityExceptionNotifier exceptionNotifier) {
    this.singularityDriver = singularityDriver;
    this.taskManager  = taskManager;
    this.transcoder = transcoder;
    this.exceptionNotifier = exceptionNotifier;
  }

  public void killAndRecord(SingularityTaskId taskId, RequestCleanupType requestCleanupType, Optional<String> user) {
    killAndRecord(taskId, Optional.of(requestCleanupType), Optional.<TaskCleanupType> absent(), Optional.<Long> absent(), Optional.<Integer> absent(), user);
  }

  public void killAndRecord(SingularityTaskId taskId, TaskCleanupType taskCleanupType, Optional<String> user) {
    killAndRecord(taskId, Optional.<RequestCleanupType> absent(), Optional.of(taskCleanupType), Optional.<Long> absent(), Optional.<Integer> absent(), user);
  }

  public void killAndRecord(SingularityTaskId taskId, Optional<RequestCleanupType> requestCleanupType, Optional<TaskCleanupType> taskCleanupType, Optional<Long> originalTimestamp, Optional<Integer> retries, Optional<String> user) {
    Preconditions.checkState(singularityDriver.canKillTask());

    Optional<TaskCleanupType> maybeCleanupFromRequestAndTask = getTaskCleanupType(requestCleanupType, taskCleanupType);

    if (maybeCleanupFromRequestAndTask.isPresent() && (maybeCleanupFromRequestAndTask.get() == TaskCleanupType.USER_REQUESTED_DESTROY || maybeCleanupFromRequestAndTask.get() == TaskCleanupType.REQUEST_DELETING)) {
      Optional<SingularityTask> task = taskManager.getTask(taskId);
      if (task.isPresent()) {
        if (task.get().getMesosTask().hasExecutor()) {
          byte[] messageBytes = transcoder.toBytes(new SingularityTaskDestroyFrameworkMessage(taskId, user));
          singularityDriver.sendFrameworkMessage(taskId, task.get().getMesosTask().getExecutor().getExecutorId(), task.get().getMesosTask().getAgentId(), messageBytes);
        } else {
          LOG.warn("Not using custom executor, will not send framework message to destroy task");
        }
      } else {
        String message = String.format("No task data available to build kill task framework message for task %s", taskId);
        exceptionNotifier.notify(message);
        LOG.error(message);
      }
    }
    singularityDriver.kill(taskId);

    taskManager.saveKilledRecord(new SingularityKilledTaskIdRecord(taskId, System.currentTimeMillis(), originalTimestamp.or(System.currentTimeMillis()), requestCleanupType, taskCleanupType, retries.or(-1) + 1));
  }

  private Optional<TaskCleanupType> getTaskCleanupType(Optional<RequestCleanupType> requestCleanupType, Optional<TaskCleanupType> taskCleanupType) {
    if (taskCleanupType.isPresent()) {
      return taskCleanupType;
    } else {
      if (requestCleanupType.isPresent()) {
        return requestCleanupType.get().getTaskCleanupType();
      }
      return Optional.absent();
    }
  }
}
