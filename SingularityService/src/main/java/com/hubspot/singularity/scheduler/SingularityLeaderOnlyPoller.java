package com.hubspot.singularity.scheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.mesos.SchedulerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityCloser;
import com.hubspot.singularity.mesos.SingularityMesosSchedulerDelegator;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

public abstract class SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityLeaderOnlyPoller.class);

  private final SingularityExceptionNotifier exceptionNotifier;
  private final ScheduledExecutorService executorService;
  private final SingularityAbort abort;
  private final SingularityCloser closer;
  private final long pollDelay;
  private final TimeUnit pollTimeUnit;
  private final SchedulerLockType schedulerLockType;

  enum SchedulerLockType {
    LOCK, NO_LOCK
  }

  public SingularityLeaderOnlyPoller(SingularityExceptionNotifier exceptionNotifier, SingularityAbort abort, SingularityCloser closer, long pollDelay, TimeUnit pollTimeUnit, SchedulerLockType schedulerLockType) {
    this.exceptionNotifier = exceptionNotifier;
    this.abort = abort;
    this.closer = closer;
    this.schedulerLockType = schedulerLockType;
    this.pollDelay = pollDelay;
    this.pollTimeUnit = pollTimeUnit;

    this.executorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat(getClass().getSimpleName() + "-%d").build());
  }

  public void start(final SingularityMesosSchedulerDelegator mesosScheduler, final SchedulerDriver driver) {
    LOG.info("Starting a {} with a {} {} delay", getClass().getSimpleName(), pollDelay, pollTimeUnit);

    executorService.scheduleWithFixedDelay(new Runnable() {

      @Override
      public void run() {
        if (schedulerLockType == SchedulerLockType.LOCK) {
          mesosScheduler.lock();
        }

        try {
          runActionOnPoll(mesosScheduler, driver);
        } catch (Throwable t) {
          LOG.error("Caught an exception while running {} -- aborting", getClass().getSimpleName(), t);
          exceptionNotifier.notify(t);
          abort.abort();
        } finally {
          if (schedulerLockType == SchedulerLockType.LOCK) {
            mesosScheduler.release();
          }
        }
      }
    }, pollDelay, pollDelay, pollTimeUnit);
  }

  public abstract void runActionOnPoll(SingularityMesosSchedulerDelegator mesosScheduler, SchedulerDriver driver);

  public void stop() {
    closer.shutdown(getClass().getName(), executorService, 1);
  }

}
