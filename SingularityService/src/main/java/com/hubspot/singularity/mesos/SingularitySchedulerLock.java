package com.hubspot.singularity.mesos;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;

public class SingularitySchedulerLock {

  private static final Logger LOG = LoggerFactory.getLogger(SingularitySchedulerLock.class);

  private final ReentrantLock stateLock;
  private final ReentrantLock offersLock;
  private final ConcurrentHashMap<String, ReentrantLock> requestLocks;

  @Inject
  public SingularitySchedulerLock() {
    this.stateLock = new ReentrantLock();
    this.offersLock = new ReentrantLock();
    this.requestLocks = new ConcurrentHashMap<>();
  }

  public long lock(String requestId, String name) {
    synchronized (stateLock) {
      while (stateLock.isLocked()) {
        try {
          stateLock.wait();
        } catch (InterruptedException ie) {
          LOG.error("Interrupted while waiting for global lock", ie);
          throw new RuntimeException(ie);
        }
      }
    }
    final long start = System.currentTimeMillis();
    LOG.info("{} - Locking {}", name, requestId);
    ReentrantLock lock = requestLocks.computeIfAbsent(requestId, (r) -> new ReentrantLock());
    lock.lock();
    LOG.info("{} - Acquired lock on {} ({})", name, requestId, JavaUtils.duration(start));
    return System.currentTimeMillis();
  }

  public void unlock(String requestId, String name, long start) {
    LOG.info("{} - Unlocking {} ({})", name, requestId, JavaUtils.duration(start));
    ReentrantLock lock = requestLocks.computeIfAbsent(requestId, (r) -> new ReentrantLock());
    lock.unlock();
  }

  public void runWithRequestLock(Runnable function, String requestId, String name) {
    long start = lock(requestId, name);
    try {
      function.run();
    } finally {
      unlock(requestId, name, start);
    }
  }

  public long lockState(String name) {
    final long start = System.currentTimeMillis();
    LOG.info("{} - Locking state lock", name);
    synchronized (stateLock) {
      stateLock.lock();
    }
    LOG.info("{} - Acquired state lock ({})", name, JavaUtils.duration(start));
    return System.currentTimeMillis();
  }

  public void unlockState(String name, long start) {
    LOG.info("{} - Unlocking state lock ({})", name, JavaUtils.duration(start));
    synchronized (stateLock) {
      stateLock.unlock();
    }
  }

  public long lockOffers(String name) {
    final long start = System.currentTimeMillis();
    LOG.info("{} - Locking offers lock", name);
    synchronized (offersLock) {
      offersLock.lock();
    }
    LOG.info("{} - Acquired offers lock ({})", name, JavaUtils.duration(start));
    return System.currentTimeMillis();
  }

  public void unlockOffers(String name, long start) {
    LOG.info("{} - Unlocking offers lock ({})", name, JavaUtils.duration(start));
    synchronized (offersLock) {
      offersLock.unlock();
    }
  }

}
