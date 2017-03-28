package com.hubspot.singularity.mesos;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;

public class SingularitySchedulerLock {

  private static final Logger LOG = LoggerFactory.getLogger(SingularitySchedulerLock.class);

  private final ReentrantLock lock;
  private long start;

  @Inject
  public SingularitySchedulerLock() {
    this.lock = new ReentrantLock();
  }

  public void lock(String name) {
    start = System.currentTimeMillis();
    LOG.info("{} locking", name);
    lock.lock();
    LOG.info("{} acquired lock after {}", name, JavaUtils.duration(start));
  }

  public void unlock(String name) {
    LOG.info("{} unlocking after {}", name, JavaUtils.duration(start));
    lock.unlock();
  }

}
