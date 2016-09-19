package com.hubspot.singularity.scheduler;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityAbort.AbortReason;
import com.hubspot.singularity.SingularityManagedScheduledExecutorServiceFactory;
import com.hubspot.singularity.mesos.SingularityMesosSchedulerDelegator;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

import io.dropwizard.lifecycle.Managed;

public abstract class SingularityLeaderOnlyPoller implements Managed {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityLeaderOnlyPoller.class);

  private final long pollDelay;
  private final TimeUnit pollTimeUnit;
  private final Optional<Lock> lockHolder;

  private ScheduledExecutorService executorService;
  private LeaderLatch leaderLatch;
  private SingularityExceptionNotifier exceptionNotifier;
  private SingularityAbort abort;
  private SingularityMesosSchedulerDelegator mesosScheduler;

  protected SingularityLeaderOnlyPoller(long pollDelay, TimeUnit pollTimeUnit) {
    this(pollDelay, pollTimeUnit, Optional.<Lock> absent());
  }

  protected SingularityLeaderOnlyPoller(long pollDelay, TimeUnit pollTimeUnit, Lock lock) {
    this(pollDelay, pollTimeUnit, Optional.of(lock));
  }

  private SingularityLeaderOnlyPoller(long pollDelay, TimeUnit pollTimeUnit, Optional<Lock> lockHolder) {
    this.pollDelay = pollDelay;
    this.pollTimeUnit = pollTimeUnit;
    this.lockHolder = lockHolder;
  }

  @Inject
  void injectPollerDependencies(SingularityManagedScheduledExecutorServiceFactory executorServiceFactory,
      LeaderLatch leaderLatch,
      SingularityExceptionNotifier exceptionNotifier,
      SingularityAbort abort,
      SingularityMesosSchedulerDelegator mesosScheduler) {
    this.executorService = executorServiceFactory.get(getClass().getSimpleName());
    this.leaderLatch = checkNotNull(leaderLatch, "leaderLatch is null");
    this.exceptionNotifier = checkNotNull(exceptionNotifier, "exceptionNotifier is null");
    this.abort = checkNotNull(abort, "abort is null");
    this.mesosScheduler = checkNotNull(mesosScheduler, "mesosScheduler is null");
  }

  @Override
  public void start() {
    if (!isEnabled()) {
      LOG.info("{} is not enabled, not starting.", getClass().getSimpleName());
      return;
    }

    if (pollDelay < 1) {
      LOG.warn("Not running {} due to delay value of {}", getClass().getSimpleName(), pollDelay);
      return;
    }

    LOG.info("Starting a {} with a {} delay", getClass().getSimpleName(), JavaUtils.durationFromMillis(pollTimeUnit.toMillis(pollDelay)));

    executorService.scheduleWithFixedDelay(new Runnable() {

      @Override
      public void run() {
        runActionIfLeaderAndMesosIsRunning();
      }

    }, pollDelay, pollDelay, pollTimeUnit);
  }

  private void runActionIfLeaderAndMesosIsRunning() {
    final boolean leadership = leaderLatch.hasLeadership();
    final boolean schedulerRunning = mesosScheduler.isRunning();

    if (!leadership || !schedulerRunning || !isEnabled()) {
      LOG.trace("Skipping {} (period: {}) (leadership: {}, mesos running: {}, enabled: {})", getClass().getSimpleName(), JavaUtils.durationFromMillis(pollTimeUnit.toMillis(pollDelay)), leadership,
          schedulerRunning, isEnabled());
      return;
    }

    LOG.trace("Running {} (period: {})", getClass().getSimpleName(), JavaUtils.durationFromMillis(pollTimeUnit.toMillis(pollDelay)));

    final long start = System.currentTimeMillis();

    if (lockHolder.isPresent()) {
      lockHolder.get().lock();
    }

    try {
      runActionOnPoll();
    } catch (Throwable t) {
      LOG.error("Caught an exception while running {}", getClass().getSimpleName(), t);
      exceptionNotifier.notify(String.format("Caught an exception while running %s", getClass().getSimpleName()), t);
      if (abortsOnError()) {
        abort.abort(AbortReason.UNRECOVERABLE_ERROR, Optional.of(t));
      }
    } finally {
      if (lockHolder.isPresent()) {
        lockHolder.get().unlock();
      }

      LOG.debug("Ran {} in {}", getClass().getSimpleName(), JavaUtils.duration(start));
    }
  }

  protected boolean isEnabled() {
    return true;
  }

  protected boolean abortsOnError() {
    return true;
  }

  public abstract void runActionOnPoll();

  @Override
  public void stop() {
  }
}
