package com.hubspot.singularity.data.history;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityCloseable;
import com.hubspot.singularity.SingularityCloser;
import com.hubspot.singularity.SingularityStartable;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

public class SingularityHistoryPersister extends SingularityCloseable<ScheduledExecutorService> implements SingularityStartable {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityHistoryPersister.class);

  private final SingularityTaskHistoryPersister taskPersister;
  private final SingularityDeployHistoryPersister deployPersister;
  private final SingularityRequestHistoryPersister requestHistoryPersister;
  private final ScheduledExecutorService executorService;
  private final SingularityConfiguration configuration;
  private final SingularityExceptionNotifier exceptionNotifier;

  @Inject
  public SingularityHistoryPersister(SingularityExceptionNotifier exceptionNotifier, SingularityTaskHistoryPersister taskPersister, SingularityRequestHistoryPersister requestHistoryPersister, SingularityDeployHistoryPersister deployPersister, SingularityConfiguration configuration, SingularityCloser closer) {
    super(closer);

    this.taskPersister = taskPersister;
    this.deployPersister = deployPersister;
    this.exceptionNotifier = exceptionNotifier;
    this.requestHistoryPersister = requestHistoryPersister;
    this.configuration = configuration;

    this.executorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("SingularityHistoryPersister-%d").build());
  }

  @Override
  public Optional<ScheduledExecutorService> getExecutorService() {
    return Optional.of(executorService);
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
