package com.hubspot.singularity.mesos;

import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.helpers.TaskLagGuardrail;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingularitySchedulerLock {
  private static final Logger LOG = LoggerFactory.getLogger(
    SingularitySchedulerLock.class
  );

  private final ReentrantLock stateLock;
  private final ReentrantLock offersLock;
  private final ConcurrentHashMap<String, ReentrantLock> requestLocks;
  private final TaskLagGuardrail taskLag;

  @Inject
  public SingularitySchedulerLock(TaskLagGuardrail taskLag) {
    this.taskLag = taskLag;
    this.stateLock = new ReentrantLock();
    this.offersLock = new ReentrantLock();
    this.requestLocks = new ConcurrentHashMap<>();
  }

  public long lock(String requestId, String name) {
    final long start = System.currentTimeMillis();
    LOG.trace("{} - Locking {}", name, requestId);
    ReentrantLock lock = requestLocks.computeIfAbsent(
      requestId,
      r -> new ReentrantLock()
    );
    lock.lock();
    LOG.trace(
      "{} - Acquired lock on {} ({})",
      name,
      requestId,
      JavaUtils.duration(start)
    );
    return System.currentTimeMillis();
  }

  /** Returns start time of lock if successful, -1 otherwise. */
  private long tryLock(String requestId, String name, long timeout, TimeUnit timeunit) {
    final long start = System.currentTimeMillis();
    try {
      LOG.trace("{} - TryLocking {}", name, requestId);
      ReentrantLock lock = requestLocks.computeIfAbsent(
        requestId,
        r -> new ReentrantLock()
      );
      boolean locked = lock.tryLock(timeout, timeunit);
      if (locked) {
        LOG.trace(
          "{} - Acquired lock on {} ({})",
          name,
          requestId,
          JavaUtils.duration(start)
        );
        return start;
      } else {
        LOG.info(
          "{} - Failed to acquire lock on {} ({})",
          name,
          requestId,
          JavaUtils.duration(start)
        );
        return -1;
      }
    } catch (InterruptedException e) {
      LOG.info(
        "{} - Interrupted trying to acquire lock on {} ({})",
        name,
        requestId,
        JavaUtils.duration(start)
      );
      Thread.currentThread().interrupt();
      return -1;
    }
  }

  public void unlock(String requestId, String name, long start) {
    long duration = System.currentTimeMillis() - start;
    if (duration > 1000) {
      LOG.debug("{} - Unlocking {} after {}ms", name, requestId, duration);
    } else {
      LOG.trace("{} - Unlocking {} after {}ms", name, requestId, duration);
    }
    ReentrantLock lock = requestLocks.computeIfAbsent(
      requestId,
      r -> new ReentrantLock()
    );
    lock.unlock();
  }

  /**
   * Run the given function with the specified request lock.
   *
   * @param function The function to run.
   * @param requestId Request to lock.
   * @param name Description of this request lock.
   */
  public void runWithRequestLock(Runnable function, String requestId, String name) {
    runWithRequestLock(function, requestId, name, Priority.HIGH);
  }

  /**
   * Run the given function with the specified request lock, unless run with low priority.
   * If run with low priority, the function will not run if the request is lagged
   * to allow higher priority components to acquire the lock.
   *
   * @param function The function to run.
   * @param requestId Request to lock.
   * @param name Description of this request lock.
   * @param priority Priority of this request lock.
   */
  public void runWithRequestLock(
    Runnable function,
    String requestId,
    String name,
    Priority priority
  ) {
    if (priority == Priority.LOW && isLocked(requestId) && taskLag.isLagged(requestId)) {
      LOG.info("{} - Skipping low priority lock on {}", name, requestId);
      return;
    }

    long start = lock(requestId, name);
    try {
      function.run();
    } finally {
      unlock(requestId, name, start);
    }
  }

  public void tryRunWithRequestLock(
    Runnable function,
    String requestId,
    String name,
    long timeout,
    TimeUnit timeunit
  ) {
    long start = tryLock(requestId, name, timeout, timeunit);
    if (start > 0) {
      try {
        function.run();
      } finally {
        unlock(requestId, name, start);
      }
    }
  }

  public <T> T runWithRequestLockAndReturn(
    Callable<T> function,
    String requestId,
    String name
  ) {
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

  public void runWithOffersLock(Runnable function, String name) {
    long start = lockOffers(name);
    try {
      function.run();
    } finally {
      unlockOffers(name, start);
    }
  }

  public void runWithOffersLockAndtimeout(
    Function<Boolean, Void> function,
    String name,
    long timeoutMillis
  ) {
    final long start = System.currentTimeMillis();
    LOG.debug("{} - Locking offers lock", name);
    try {
      boolean acquired = offersLock.tryLock(timeoutMillis, TimeUnit.MILLISECONDS);
      LOG.debug(
        "{} - Acquired offers lock ({}) ({})",
        name,
        acquired,
        JavaUtils.duration(start)
      );
      long functionStart = System.currentTimeMillis();
      try {
        function.apply(acquired);
      } finally {
        if (acquired) {
          unlockOffers(name, functionStart);
        }
      }
    } catch (InterruptedException ie) {
      LOG.warn("Interrupted waiting for offer lock", ie);
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

  private boolean isLocked(String requestId) {
    ReentrantLock lock = requestLocks.get(requestId);
    return lock != null && lock.isLocked();
  }

  public enum Priority {
    LOW,
    HIGH
  }
}
