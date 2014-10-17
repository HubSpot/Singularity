package com.hubspot.singularity;

import io.dropwizard.lifecycle.Managed;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Singleton;

import org.apache.mesos.Protos;
import org.apache.mesos.Protos.MasterInfo;
import org.apache.mesos.Protos.Status;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.hubspot.singularity.SingularityRequestCleanup.RequestCleanupType;
import com.hubspot.singularity.SingularityTaskCleanup.TaskCleanupType;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.mesos.SingularityDriver;

@Singleton
public class SingularityDriverManager implements Managed {

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
    this.driver = Optional.absent();
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

  @VisibleForTesting
  public SingularityDriver getDriver() {
    return driver.get();
  }

  public Optional<MasterInfo> getMaster() {
    driverLock.lock();

    try {
      if (!driver.isPresent()) {
        return Optional.absent();
      }

      return Optional.fromNullable(driver.get().getMaster());
    } finally {
      driverLock.unlock();
    }
  }

  public Optional<Long> getLastOfferTimestamp() {
    driverLock.lock();

    try {
      if (!driver.isPresent()) {
        return Optional.absent();
      }

      return driver.get().getLastOfferTimestamp();
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

      currentStatus = driver.get().kill(taskId);

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

  public Protos.Status stopMesos() {
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
