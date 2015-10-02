package com.hubspot.singularity.oomkiller;

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.oomkiller.config.SingularityOOMKillerConfiguration;
import com.hubspot.singularity.runner.base.sentry.SingularityRunnerExceptionNotifier;
import com.hubspot.singularity.runner.base.shared.SingularityDriver;

public class SingularityOOMKillerDriver implements SingularityDriver {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityOOMKillerDriver.class);

  private final SingularityOOMKillerConfiguration configuration;
  private final ScheduledExecutorService scheduler;
  private final SingularityOOMKiller oomKiller;
  private final SingularityRunnerExceptionNotifier exceptionNotifier;

  private ScheduledFuture<?> future;

  @Inject
  public SingularityOOMKillerDriver(SingularityOOMKillerConfiguration configuration, SingularityOOMKiller oomKiller, SingularityRunnerExceptionNotifier exceptionNotifier) {
    this.configuration = configuration;
    this.oomKiller = oomKiller;
    this.exceptionNotifier = exceptionNotifier;

    this.scheduler = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("SingularityOOMKillerDriver-%d").build());
  }

  @Override
  public void startAndWait() {
    LOG.info("Starting an OOMKiller that will run every {}", JavaUtils.durationFromMillis(configuration.getCheckForOOMEveryMillis()));

    future = this.scheduler.scheduleAtFixedRate(new Runnable() {

      @Override
      public void run() {
        final long start = System.currentTimeMillis();

        try {
          oomKiller.checkForOOMS();

        } catch (Throwable t) {
          LOG.error("Uncaught exception while checking for OOMS", t);
          exceptionNotifier.notify(t, Collections.<String, String>emptyMap());
        } finally {
          LOG.info("Finished checking OOMS after {}", JavaUtils.duration(start));
        }

      }
    }, configuration.getCheckForOOMEveryMillis(), configuration.getCheckForOOMEveryMillis(), TimeUnit.MILLISECONDS);

    try {
      future.get();
    } catch (InterruptedException | ExecutionException e) {
      LOG.warn("Unexpected exception while waiting on future", e);
      exceptionNotifier.notify(e, Collections.<String, String>emptyMap());
    }

  }

  @Override
  public void shutdown() {
    future.cancel(true);
    scheduler.shutdown();
  }

}
