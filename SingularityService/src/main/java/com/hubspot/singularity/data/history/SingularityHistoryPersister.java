package com.hubspot.singularity.data.history;

import io.dropwizard.lifecycle.Managed;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.scheduler.SingularityLeaderOnlyPoller;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

@Singleton
public class SingularityHistoryPersister extends SingularityLeaderOnlyPoller implements Managed {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityHistoryPersister.class);

  private final SingularityTaskHistoryPersister taskPersister;
  private final SingularityDeployHistoryPersister deployPersister;
  private final SingularityRequestHistoryPersister requestHistoryPersister;
  private final SingularityExceptionNotifier exceptionNotifier;

  @Inject
  public SingularityHistoryPersister(SingularityExceptionNotifier exceptionNotifier, SingularityTaskHistoryPersister taskPersister,
      SingularityRequestHistoryPersister requestHistoryPersister, SingularityDeployHistoryPersister deployPersister, SingularityConfiguration configuration) {
    super(configuration.getPersistHistoryEverySeconds(), TimeUnit.SECONDS, configuration.getDatabaseConfiguration().isPresent());

    this.taskPersister = taskPersister;
    this.deployPersister = deployPersister;
    this.exceptionNotifier = exceptionNotifier;
    this.requestHistoryPersister = requestHistoryPersister;
  }

  @Override
  public void runActionOnPoll() {
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
}
