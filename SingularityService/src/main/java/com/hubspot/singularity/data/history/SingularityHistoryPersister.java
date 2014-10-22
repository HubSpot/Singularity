package com.hubspot.singularity.data.history;

import io.dropwizard.lifecycle.Managed;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

@Singleton
public class SingularityHistoryPersister implements Managed {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityHistoryPersister.class);

  private final SingularityTaskHistoryPersister taskPersister;
  private final SingularityDeployHistoryPersister deployPersister;
  private final SingularityRequestHistoryPersister requestHistoryPersister;
  private final ScheduledExecutorService executorService;
  private final SingularityConfiguration configuration;
  private final SingularityExceptionNotifier exceptionNotifier;

  @Inject
  public SingularityHistoryPersister(SingularityExceptionNotifier exceptionNotifier, SingularityTaskHistoryPersister taskPersister, SingularityRequestHistoryPersister requestHistoryPersister, SingularityDeployHistoryPersister deployPersister, SingularityConfiguration configuration) {
    this.taskPersister = taskPersister;
    this.deployPersister = deployPersister;
    this.exceptionNotifier = exceptionNotifier;
    this.requestHistoryPersister = requestHistoryPersister;
    this.configuration = configuration;

    this.executorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("SingularityHistoryPersister-%d").build());
  }

  @Override
  public void stop() {
    MoreExecutors.shutdownAndAwaitTermination(executorService, 1, TimeUnit.SECONDS);
  }

  @Override
  public void start() {
    if (configuration.getPersistHistoryEverySeconds() < 1) {
      LOG.warn("Not persisting history because persistHistoryEverySeconds is set to {}", configuration.getPersistHistoryEverySeconds());
      return;
    }

    LOG.info("Starting a history persister with a {} delay", JavaUtils.durationFromMillis(TimeUnit.SECONDS.toMillis(configuration.getPersistHistoryEverySeconds())));

    executorService.scheduleWithFixedDelay(new Runnable() {

      @Override
      public void run() {
        try {
          taskPersister.checkInactiveTaskIds();
        } catch (Throwable t) {
          exceptionNotifier.notify(t);
          LOG.error("While persisting task history", t);
        }
        try {
          deployPersister.checkInactiveDeploys();
        } catch (Throwable t) {
          exceptionNotifier.notify(t);
          LOG.error("While persisting deploy history", t);
        }
        try {
          requestHistoryPersister.checkRequestHistory();
        } catch (Throwable t) {
          exceptionNotifier.notify(t);
          LOG.error("While persisting request history", t);
        }
      }
    }, configuration.getPersistHistoryEverySeconds(), configuration.getPersistHistoryEverySeconds(), TimeUnit.SECONDS);
  }
}
