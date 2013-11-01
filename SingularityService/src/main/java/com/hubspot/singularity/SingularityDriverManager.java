package com.hubspot.singularity;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.mesos.Protos;

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
      newStatus = driver.abort();
      driver = null;
    } finally {
      driverLock.unlock();
    }
    
    return newStatus;
  }
  
}
