package com.hubspot.singularity;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Singleton;

import org.apache.mesos.Protos;
import org.apache.mesos.Protos.MasterInfo;
import org.apache.mesos.Protos.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.transcoders.Transcoder;
import com.hubspot.singularity.mesos.SingularityDriver;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

import io.dropwizard.lifecycle.Managed;

@Singleton
public class SingularityDriverManager implements Managed {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityDriverManager.class);

  private final SingularityDriver driver;
  private final TaskManager taskManager;
  private final Lock driverLock;
  private final Transcoder<SingularityTaskDestroyFrameworkMessage> transcoder;
  private final SingularityExceptionNotifier exceptionNotifier;

  private Protos.Status currentStatus;

  @Inject
  public SingularityDriverManager(SingularityDriver driver, TaskManager taskManager, Transcoder<SingularityTaskDestroyFrameworkMessage> transcoder, SingularityExceptionNotifier exceptionNotifier) {
    this.taskManager = taskManager;

    this.driverLock = new ReentrantLock();

    this.currentStatus = Protos.Status.DRIVER_NOT_STARTED;
    this.driver = driver;
    this.transcoder = transcoder;
    this.exceptionNotifier = exceptionNotifier;
  }

  @Override
  public void start() {
  }

  @Override
  public void stop() {
    stopMesos();
  }

  public Protos.Status getCurrentStatus() {
    return currentStatus;
  }

  public Optional<MasterInfo> getMaster() {
    driverLock.lock();

    try {
      return driver.getMaster();
    } finally {
      driverLock.unlock();
    }
  }

  public Optional<Long> getLastOfferTimestamp() {
    driverLock.lock();

    try {
      return driver.getLastOfferTimestamp();
    } finally {
      driverLock.unlock();
    }
  }

  public Protos.Status killAndRecord(SingularityTaskId taskId, RequestCleanupType requestCleanupType, Optional<String> user) {
    return killAndRecord(taskId, Optional.of(requestCleanupType), Optional.<TaskCleanupType> absent(), Optional.<Long> absent(), Optional.<Integer> absent(), user);
  }

  public Protos.Status killAndRecord(SingularityTaskId taskId, TaskCleanupType taskCleanupType, Optional<String> user) {
    return killAndRecord(taskId, Optional.<RequestCleanupType> absent(), Optional.of(taskCleanupType), Optional.<Long> absent(), Optional.<Integer> absent(), user);
  }

  public Protos.Status killAndRecord(SingularityTaskId taskId, Optional<RequestCleanupType> requestCleanupType, Optional<TaskCleanupType> taskCleanupType, Optional<Long> originalTimestamp, Optional<Integer> retries, Optional<String> user) {
    driverLock.lock();

    try {
      Preconditions.checkState(canKillTask());

      Optional<TaskCleanupType> maybeCleanupFromRequestAndTask = getTaskCleanupType(requestCleanupType, taskCleanupType);

      if (maybeCleanupFromRequestAndTask.isPresent() && maybeCleanupFromRequestAndTask.get() == TaskCleanupType.USER_REQUESTED_DESTROY) {
        Optional<SingularityTask> task = taskManager.getTask(taskId);
        if (task.isPresent()) {
          if (task.get().getMesosTask().hasExecutor()) {
            byte[] messageBytes = transcoder.toBytes(new SingularityTaskDestroyFrameworkMessage(taskId, user));
            driver.sendFrameworkMessage(taskId, task.get().getMesosTask().getExecutor().getExecutorId(), task.get().getMesosTask().getSlaveId(), messageBytes);
          } else {
            LOG.warn("Not using custom executor, will not send framework message to destroy task");
          }
        } else {
          String message = String.format("No task data available to build kill task framework message for task %s", taskId);
          exceptionNotifier.notify(message);
          LOG.error(message);
        }
      }
      currentStatus = driver.kill(taskId);

      taskManager.saveKilledRecord(new SingularityKilledTaskIdRecord(taskId, System.currentTimeMillis(), originalTimestamp.or(System.currentTimeMillis()), requestCleanupType, taskCleanupType, retries.or(-1) + 1));

      Preconditions.checkState(currentStatus == Status.DRIVER_RUNNING);
    } finally {
      driverLock.unlock();
    }

    return currentStatus;
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

  public Protos.Status startMesos() {
    driverLock.lock();

    try {
      Preconditions.checkState(isStartable());

      currentStatus = driver.start();
    } finally {
      driverLock.unlock();
    }

    return currentStatus;
  }

  private boolean canKillTask() {
    return currentStatus == Status.DRIVER_RUNNING;
  }

  private boolean isStartable() {
    return currentStatus == Status.DRIVER_NOT_STARTED;
  }

  private boolean isStoppable() {
    return currentStatus == Status.DRIVER_RUNNING;
  }

  public Protos.Status stopMesos() {
    driverLock.lock();

    try {
      if (isStoppable()) {
        currentStatus = driver.abort();
      }
    } finally {
      driverLock.unlock();
    }

    return currentStatus;
  }

}
