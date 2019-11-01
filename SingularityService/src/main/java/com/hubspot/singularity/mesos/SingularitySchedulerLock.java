package com.hubspot.singularity.mesos;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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
  private final ConcurrentHashMap<String, Long> lockTimes;

  @Inject
  public SingularitySchedulerLock() {
    this.stateLock = new ReentrantLock();
    this.offersLock = new ReentrantLock();
    this.requestLocks = new ConcurrentHashMap<>();
    this.lockTimes = new ConcurrentHashMap<>();
  }

  private long lock(String requestId, String name) {
    final long start = System.currentTimeMillis();
    LOG.trace("{} - Locking {}", name, requestId);
    ReentrantLock lock = requestLocks.computeIfAbsent(requestId, (r) -> new ReentrantLock());
    lock.lock();
    LOG.trace("{} - Acquired lock on {} ({})", name, requestId, JavaUtils.duration(start));
    return System.currentTimeMillis();
  }

  private void unlock(String requestId, String name, long start) {
    long duration = System.currentTimeMillis() - start;
    if (duration > 1000) {
      LOG.debug("{} - Unlocking {} after {}ms", name, requestId, duration);
    } else {
      LOG.trace("{} - Unlocking {} after {}ms", name, requestId, duration);
    }
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

  public void runWithRequestLockAndTimeout(Runnable function, long timeout, String requestId, String name, Runnable functionOnTimeout) {
    final long start = System.currentTimeMillis();
    LOG.trace("{} - Locking {}", name, requestId);
    ReentrantLock lock = requestLocks.computeIfAbsent(requestId, (r) -> new ReentrantLock());
    try {
      if (lock.tryLock(timeout, TimeUnit.MILLISECONDS)) {
        try {
          LOG.trace("{} - Acquired lock on {} ({})", name, requestId, JavaUtils.duration(start));
          function.run();
        } finally {
          unlock(requestId, name, start);
        }
      } else {
        functionOnTimeout.run();
      }
    } catch (InterruptedException ie) {
      LOG.warn("Interrupted waiting for request lock on {}", requestId);
      functionOnTimeout.run();
    }
  }

  public <T> T runWithRequestLockAndReturn(Callable<T> function, String requestId, String name) {
    long start = lock(requestId, name);
    try {
      return function.call();
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      unlock(requestId, name, start);
    }
  }

  void runWithStateLock(Runnable function, String name) {
    long start = lockState(name);
    try {
      function.run();
    } finally {
      unlockState(name, start);
    }
  }

  private long lockState(String name) {
    final long start = System.currentTimeMillis();
    LOG.info("{} - Locking state lock", name);
    stateLock.lock();
    LOG.info("{} - Acquired state lock ({})", name, JavaUtils.duration(start));
    return System.currentTimeMillis();
  }

  private void unlockState(String name, long start) {
    LOG.info("{} - Unlocking state lock ({})", name, JavaUtils.duration(start));
    stateLock.unlock();
  }

  public void runWithOffersLock(Runnable function,  String name) {
    long start = lockOffers(name);
    try {
      function.run();
    } finally {
      unlockOffers(name, start);
    }
  }

  private long lockOffers(String name) {
    final long start = System.currentTimeMillis();
    LOG.debug("{} - Locking offers lock", name);
    offersLock.lock();
    LOG.debug("{} - Acquired offers lock ({})", name, JavaUtils.duration(start));
    return System.currentTimeMillis();
  }

  private void unlockOffers(String name, long start) {
    LOG.debug("{} - Unlocking offers lock ({})", name, JavaUtils.duration(start));
    offersLock.unlock();
  }

}
