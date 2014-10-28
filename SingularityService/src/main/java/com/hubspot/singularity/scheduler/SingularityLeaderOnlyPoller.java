package com.hubspot.singularity.scheduler;

import io.dropwizard.lifecycle.Managed;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityAbort.AbortReason;
import com.hubspot.singularity.mesos.SingularityMesosSchedulerDelegator;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

public abstract class SingularityLeaderOnlyPoller implements Managed {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityLeaderOnlyPoller.class);

  private final LeaderLatch leaderLatch;
  private final SingularityMesosSchedulerDelegator mesosScheduler;
  private final SingularityExceptionNotifier exceptionNotifier;
  private final ScheduledExecutorService executorService;
  private final SingularityAbort abort;
  private final long pollDelay;
  private final TimeUnit pollTimeUnit;
  private final SchedulerLockType schedulerLockType;

  enum SchedulerLockType {
    LOCK, NO_LOCK
  }

  protected SingularityLeaderOnlyPoller(LeaderLatch leaderLatch, SingularityMesosSchedulerDelegator mesosScheduler, SingularityExceptionNotifier exceptionNotifier, SingularityAbort abort,
      long pollDelay, TimeUnit pollTimeUnit, SchedulerLockType schedulerLockType) {
    this.leaderLatch = leaderLatch;
    this.mesosScheduler = mesosScheduler;
    this.exceptionNotifier = exceptionNotifier;
    this.abort = abort;
    this.schedulerLockType = schedulerLockType;
    this.pollDelay = pollDelay;
    this.pollTimeUnit = pollTimeUnit;

    this.executorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat(getClass().getSimpleName() + "-%d").build());
  }

  @Override
  public void start() {
    LOG.info("Starting a {} with a {} delay", getClass().getSimpleName(), JavaUtils.durationFromMillis(pollTimeUnit.toMillis(pollDelay)));

    executorService.scheduleWithFixedDelay(new Runnable() {

      @Override
      public void run() {
        runActionIfLeaderAndMesosIsRunning();
      }

    }, pollDelay, pollDelay, pollTimeUnit);
  }

  private void runActionIfLeaderAndMesosIsRunning() {
    if (!leaderLatch.hasLeadership() || !mesosScheduler.isRunning()) {
      return;
    }

    LOG.trace("Running {} (period: {})", getClass().getSimpleName(), JavaUtils.durationFromMillis(pollTimeUnit.toMillis(pollDelay)));

    if (schedulerLockType == SchedulerLockType.LOCK) {
      mesosScheduler.lock();
    }

    try {
      runActionOnPoll();
    } catch (Throwable t) {
      LOG.error("Caught an exception while running {} -- aborting", getClass().getSimpleName(), t);
      exceptionNotifier.notify(t);
      abort.abort(AbortReason.UNRECOVERABLE_ERROR);
    } finally {
      if (schedulerLockType == SchedulerLockType.LOCK) {
        mesosScheduler.release();
      }
    }
  }

  public abstract void runActionOnPoll();

  @Override
  public void stop() {
    MoreExecutors.shutdownAndAwaitTermination(executorService, 1, TimeUnit.SECONDS);
  }
}
