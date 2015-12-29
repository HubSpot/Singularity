package com.hubspot.singularity;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Singleton;

import org.apache.mesos.Protos;
import org.apache.mesos.Protos.MasterInfo;
import org.apache.mesos.Protos.Status;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.mesos.SingularityDriver;

import io.dropwizard.lifecycle.Managed;

@Singleton
public class SingularityDriverManager implements Managed {

  private final SingularityDriver driver;
  private final TaskManager taskManager;
  private final Lock driverLock;

  private Protos.Status currentStatus;

  @Inject
  public SingularityDriverManager(SingularityDriver driver, TaskManager taskManager) {
    this.taskManager = taskManager;

    this.driverLock = new ReentrantLock();

    this.currentStatus = Protos.Status.DRIVER_NOT_STARTED;
    this.driver = driver;
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

  public Protos.Status killAndRecord(SingularityTaskId taskId, RequestCleanupType requestCleanupType) {
    return killAndRecord(taskId, Optional.of(requestCleanupType), Optional.<TaskCleanupType> absent(), Optional.<Long> absent(), Optional.<Integer> absent());
  }

  public Protos.Status killAndRecord(SingularityTaskId taskId, TaskCleanupType taskCleanupType) {
    return killAndRecord(taskId, Optional.<RequestCleanupType> absent(), Optional.of(taskCleanupType), Optional.<Long> absent(), Optional.<Integer> absent());
  }

  public Protos.Status killAndRecord(SingularityTaskId taskId, Optional<RequestCleanupType> requestCleanupType, Optional<TaskCleanupType> taskCleanupType, Optional<Long> originalTimestamp, Optional<Integer> retries) {
    driverLock.lock();

    try {
      Preconditions.checkState(canKillTask());

      currentStatus = driver.kill(taskId);

      taskManager.saveKilledRecord(new SingularityKilledTaskIdRecord(taskId, System.currentTimeMillis(), originalTimestamp.or(System.currentTimeMillis()), requestCleanupType, taskCleanupType, retries.or(-1) + 1));

      Preconditions.checkState(currentStatus == Status.DRIVER_RUNNING);
    } finally {
      driverLock.unlock();
    }

    return currentStatus;
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
