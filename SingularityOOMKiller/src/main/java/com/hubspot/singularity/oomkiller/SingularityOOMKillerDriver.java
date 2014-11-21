package com.hubspot.singularity.oomkiller;

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
import com.hubspot.singularity.runner.base.shared.SingularityDriver;

public class SingularityOOMKillerDriver implements SingularityDriver {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityOOMKillerDriver.class);

  private final SingularityOOMKillerConfiguration configuration;
  private final ScheduledExecutorService scheduler;
  private final SingularityOOMKiller oomKiller;

  private ScheduledFuture<?> future;

  @Inject
  public SingularityOOMKillerDriver(SingularityOOMKillerConfiguration configuration, SingularityOOMKiller oomKiller) {
    this.configuration = configuration;
    this.oomKiller = oomKiller;

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
        } finally {
          LOG.info("Finished checking OOMS after {}", JavaUtils.duration(start));
        }

      }
    }, configuration.getCheckForOOMEveryMillis(), configuration.getCheckForOOMEveryMillis(), TimeUnit.MILLISECONDS);

    try {
      future.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return;
    } catch (ExecutionException e) {
      LOG.warn("Unexpected exception while waiting on future", e.getCause());
    }
  }

  @Override
  public void shutdown() {
    future.cancel(true);
    scheduler.shutdown();
  }

}
