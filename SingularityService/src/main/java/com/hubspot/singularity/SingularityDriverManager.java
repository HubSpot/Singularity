package com.hubspot.singularity;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.mesos.Protos;
import org.apache.mesos.Protos.MasterInfo;
import org.apache.mesos.Protos.Status;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.hubspot.singularity.mesos.SingularityDriver;

public class SingularityDriverManager {

  private final Provider<SingularityDriver> driverProvider;
  private final Lock driverLock;

  private SingularityDriver driver;

  @Inject
  public SingularityDriverManager(Provider<SingularityDriver> driverProvider) {
    this.driverProvider = driverProvider;
    
    driverLock = new ReentrantLock();
  }
  
  @VisibleForTesting
  public SingularityDriver getDriver() {
    return driver;
  }
  
  public Optional<MasterInfo> getMaster() {
    driverLock.lock();
    
    try {
      if (driver == null) {
        return Optional.absent();
      }
      
      return Optional.fromNullable(driver.getMaster());
    } finally {
      driverLock.unlock();
    }
  }
  
  public Optional<Long> getLastOfferTimestamp() {
    driverLock.lock();
    
    try {
      if (driver == null) {
        return Optional.absent();
      }
      
      return driver.getLastOfferTimestamp();
    } finally {
      driverLock.unlock();
    } 
  }
  
  public Protos.Status kill(String taskId) {
    Protos.Status newStatus = null;
    
    driverLock.lock();
    
    try {
      Preconditions.checkState(driver != null);

      newStatus = driver.kill(taskId);
      
      Preconditions.checkState(newStatus == Status.DRIVER_RUNNING);
    } finally {
      driverLock.unlock();
    }
    
    return newStatus;
  }
  
  public Protos.Status start() {
    Protos.Status newStatus = null;
    
    driverLock.lock();
    
    try {
      Preconditions.checkState(driver == null);
      
      driver = driverProvider.get();
      
      newStatus = driver.start();
    } finally {
      driverLock.unlock();
    }
    
    return newStatus;
  }
  
  public Protos.Status stop() {
    Protos.Status newStatus = null;
    
    driverLock.lock();
    
    try {
      if (driver != null) {
        newStatus = driver.abort();
      }
    } finally {
      driverLock.unlock();
    }
    
    return newStatus;
  }
  
}
