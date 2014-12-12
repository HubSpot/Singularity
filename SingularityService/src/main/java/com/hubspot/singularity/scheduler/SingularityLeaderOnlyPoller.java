package com.hubspot.singularity.scheduler;

import static com.google.common.base.Preconditions.checkNotNull;

import io.dropwizard.lifecycle.Managed;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityAbort.AbortReason;
import com.hubspot.singularity.mesos.SingularityMesosSchedulerDelegator;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

public abstract class SingularityLeaderOnlyPoller implements Managed {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityLeaderOnlyPoller.class);

  private final ScheduledExecutorService executorService;
  private final long pollDelay;
  private final TimeUnit pollTimeUnit;
  private final boolean enabled;

  private LeaderLatch leaderLatch;
  private SingularityExceptionNotifier exceptionNotifier;
  private SingularityAbort abort;
  private SingularityMesosSchedulerDelegator mesosScheduler;

  protected Optional<Lock> lockHolder = Optional.absent();

  protected SingularityLeaderOnlyPoller(long pollDelay, TimeUnit pollTimeUnit) {
    this(pollDelay, pollTimeUnit, true);
  }

  protected SingularityLeaderOnlyPoller(long pollDelay, TimeUnit pollTimeUnit, boolean enabled) {
    this.pollDelay = pollDelay;
    this.pollTimeUnit = pollTimeUnit;
    this.enabled = enabled;

    this.executorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat(getClass().getSimpleName() + "-%d").build());
  }

  @Inject
  void injectPollerDependencies(LeaderLatch leaderLatch,
      SingularityExceptionNotifier exceptionNotifier,
      SingularityAbort abort,
      SingularityMesosSchedulerDelegator mesosScheduler) {
    this.leaderLatch = checkNotNull(leaderLatch, "leaderLatch is null");
    this.exceptionNotifier = checkNotNull(exceptionNotifier, "exceptionNotifier is null");
    this.abort = checkNotNull(abort, "abort is null");
    this.mesosScheduler = checkNotNull(mesosScheduler, "mesosScheduler is null");
  }

  @Override
  public void start() {
    if (!enabled) {
      LOG.info("{} is not enabled, skipping.", getClass().getSimpleName());
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

    if (!leadership || !schedulerRunning) {
      LOG.trace("Skipping {} (period: {}) (leadership: {}, mesos running: {})", getClass().getSimpleName(), JavaUtils.durationFromMillis(pollTimeUnit.toMillis(pollDelay)), leadership, schedulerRunning);
      return;
    }

    LOG.trace("Running {} (period: {})", getClass().getSimpleName(), JavaUtils.durationFromMillis(pollTimeUnit.toMillis(pollDelay)));

    if (lockHolder.isPresent()) {
      lockHolder.get().lock();
    }

    try {
      runActionOnPoll();
    } catch (Throwable t) {
      LOG.error("Caught an exception while running {} -- aborting", getClass().getSimpleName(), t);
      exceptionNotifier.notify(t);
      abort.abort(AbortReason.UNRECOVERABLE_ERROR);
    } finally {
      if (lockHolder.isPresent()) {
        lockHolder.get().unlock();
      }
    }
  }

  public abstract void runActionOnPoll();

  @Override
  public void stop() {
    MoreExecutors.shutdownAndAwaitTermination(executorService, 1, TimeUnit.SECONDS);
  }
}
