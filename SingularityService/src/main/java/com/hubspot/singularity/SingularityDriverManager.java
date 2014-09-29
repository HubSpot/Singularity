package com.hubspot.singularity;

import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.mesos.Protos;
import org.apache.mesos.Protos.MasterInfo;
import org.apache.mesos.Protos.Status;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.hubspot.singularity.SingularityRequestCleanup.RequestCleanupType;
import com.hubspot.singularity.SingularityTaskCleanup.TaskCleanupType;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.mesos.SingularityDriver;

public class SingularityDriverManager {

  private final Provider<SingularityDriver> driverProvider;
  private final TaskManager taskManager;
  private final Lock driverLock;

  private Optional<SingularityDriver> driver;
  private Protos.Status currentStatus;

  @Inject
  public SingularityDriverManager(Provider<SingularityDriver> driverProvider, TaskManager taskManager) {
    this.driverProvider = driverProvider;
    this.taskManager = taskManager;

    this.driverLock = new ReentrantLock();

    this.currentStatus = Protos.Status.DRIVER_NOT_STARTED;
    this.driver = Optional.empty();
  }

  public Protos.Status getCurrentStatus() {
    return currentStatus;
  }

  @VisibleForTesting
  public SingularityDriver getDriver() {
    return driver.get();
  }

  public Optional<MasterInfo> getMaster() {
    driverLock.lock();

    try {
      if (!driver.isPresent()) {
        return Optional.empty();
      }

      return Optional.ofNullable(driver.get().getMaster());
    } finally {
      driverLock.unlock();
    }
  }

  public Optional<Long> getLastOfferTimestamp() {
    driverLock.lock();

    try {
      if (!driver.isPresent()) {
        return Optional.empty();
      }

      return driver.get().getLastOfferTimestamp();
    } finally {
      driverLock.unlock();
    }
  }

  public Protos.Status killAndRecord(SingularityTaskId taskId, RequestCleanupType requestCleanupType) {
    return killAndRecord(taskId, Optional.of(requestCleanupType), Optional.empty(), Optional.empty(), Optional.empty());
  }

  public Protos.Status killAndRecord(SingularityTaskId taskId, TaskCleanupType taskCleanupType) {
    return killAndRecord(taskId, Optional.empty(), Optional.of(taskCleanupType), Optional.empty(), Optional.empty());
  }

  public Protos.Status killAndRecord(SingularityTaskId taskId, Optional<RequestCleanupType> requestCleanupType, Optional<TaskCleanupType> taskCleanupType, Optional<Long> originalTimestamp, Optional<Integer> retries) {
    driverLock.lock();

    try {
      Preconditions.checkState(canKillTask());

      currentStatus = driver.get().kill(taskId);

      taskManager.saveKilledRecord(new SingularityKilledTaskIdRecord(taskId, System.currentTimeMillis(), originalTimestamp.orElse(System.currentTimeMillis()), requestCleanupType, taskCleanupType, retries.orElse(-1) + 1));

      Preconditions.checkState(currentStatus == Status.DRIVER_RUNNING);
    } finally {
      driverLock.unlock();
    }

    return currentStatus;
  }

  public Protos.Status start() {
    driverLock.lock();

    try {
      Preconditions.checkState(isStartable());

      driver = Optional.of(driverProvider.get());

      currentStatus = driver.get().start();
    } finally {
      driverLock.unlock();
    }

    return currentStatus;
  }

  private boolean canKillTask() {
    return driver.isPresent() && currentStatus == Status.DRIVER_RUNNING;
  }

  private boolean isStartable() {
    return !driver.isPresent() && currentStatus == Status.DRIVER_NOT_STARTED;
  }

  private boolean isStoppable() {
    return driver.isPresent() && currentStatus == Status.DRIVER_RUNNING;
  }

  public Protos.Status stop() {
    driverLock.lock();

    try {
      if (isStoppable()) {
        currentStatus = driver.get().abort();
      }
    } finally {
      driverLock.unlock();
    }

    return currentStatus;
  }

}
