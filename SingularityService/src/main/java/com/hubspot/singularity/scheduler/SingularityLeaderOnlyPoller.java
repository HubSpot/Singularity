package com.hubspot.singularity.scheduler;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityAbort.AbortReason;
import com.hubspot.singularity.SingularityMainModule;
import com.hubspot.singularity.SingularityManagedScheduledExecutorServiceFactory;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.mesos.SingularityMesosScheduler;
import com.hubspot.singularity.mesos.SingularitySchedulerLock;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

import io.dropwizard.lifecycle.Managed;

public abstract class SingularityLeaderOnlyPoller implements Managed {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityLeaderOnlyPoller.class);

  private final long pollDelay;
  private final TimeUnit pollTimeUnit;
  private final Optional<SingularitySchedulerLock> lockHolder;
  private final boolean delayWhenLargeStatusUpdateDelta;

  private ScheduledExecutorService executorService;
  private LeaderLatch leaderLatch;
  private SingularityExceptionNotifier exceptionNotifier;
  private SingularityAbort abort;
  private SingularityMesosScheduler mesosScheduler;
  private long delayPollersWhenDeltaOverMs;
  private AtomicLong statusUpdateDelta30sAverage;

  protected SingularityLeaderOnlyPoller(long pollDelay, TimeUnit pollTimeUnit) {
    this(pollDelay, pollTimeUnit, Optional.absent(), false);
  }

  protected SingularityLeaderOnlyPoller(long pollDelay, TimeUnit pollTimeUnit, SingularitySchedulerLock lock, boolean delayWhenLargeStatusUpdateDelta) {
    this(pollDelay, pollTimeUnit, Optional.of(lock), delayWhenLargeStatusUpdateDelta);
  }

  private SingularityLeaderOnlyPoller(long pollDelay, TimeUnit pollTimeUnit, Optional<SingularitySchedulerLock> lockHolder, boolean delayWhenLargeStatusUpdateDelta) {
    this.pollDelay = pollDelay;
    this.pollTimeUnit = pollTimeUnit;
    this.lockHolder = lockHolder;
    this.delayWhenLargeStatusUpdateDelta = delayWhenLargeStatusUpdateDelta;
  }

  @Inject
  void injectPollerDependencies(SingularityManagedScheduledExecutorServiceFactory executorServiceFactory,
                                LeaderLatch leaderLatch,
                                SingularityExceptionNotifier exceptionNotifier,
                                SingularityAbort abort,
                                SingularityMesosScheduler mesosScheduler,
                                SingularityConfiguration configuration,
                                @Named(SingularityMainModule.STATUS_UPDATE_DELTA_30S_AVERAGE) AtomicLong statusUpdateDelta30sAverage) {
    this.executorService = executorServiceFactory.get(getClass().getSimpleName());
    this.leaderLatch = checkNotNull(leaderLatch, "leaderLatch is null");
    this.exceptionNotifier = checkNotNull(exceptionNotifier, "exceptionNotifier is null");
    this.abort = checkNotNull(abort, "abort is null");
    this.mesosScheduler = checkNotNull(mesosScheduler, "mesosScheduler is null");
    this.delayPollersWhenDeltaOverMs = configuration.getDelayPollersWhenDeltaOverMs();
    this.statusUpdateDelta30sAverage = checkNotNull(statusUpdateDelta30sAverage, "statusUpdateDeltaAverage is null");
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

    if (delayWhenLargeStatusUpdateDelta && statusUpdateDelta30sAverage.get() > delayPollersWhenDeltaOverMs) {
      LOG.info("Delaying run of {} until status updates have caught up", getClass().getSimpleName());
      return;
    }

    LOG.trace("Running {} (period: {})", getClass().getSimpleName(), JavaUtils.durationFromMillis(pollTimeUnit.toMillis(pollDelay)));

    long start = System.currentTimeMillis();

    if (lockHolder.isPresent()) {
      start = lockHolder.get().lock(getClass().getSimpleName());
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
        lockHolder.get().unlock(getClass().getSimpleName(), start);
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
