package com.hubspot.singularity.mesos;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;

public class SingularitySchedulerLock {

  private static final Logger LOG = LoggerFactory.getLogger(SingularitySchedulerLock.class);

  private final ReentrantLock lock;

  @Inject
  public SingularitySchedulerLock() {
    this.lock = new ReentrantLock();
  }

  public long lock(String name) {
    final long start = System.currentTimeMillis();
    LOG.info("{} - Locking", name);
    lock.lock();
    LOG.info("{} - Acquired ({})", name, JavaUtils.duration(start));
    return System.currentTimeMillis();
  }

  public void unlock(String name, long start) {
    LOG.info("{} - Unlocking ({})", name, JavaUtils.duration(start));
    lock.unlock();
  }

}
