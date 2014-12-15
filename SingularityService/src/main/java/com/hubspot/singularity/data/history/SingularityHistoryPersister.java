package com.hubspot.singularity.data.history;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.scheduler.SingularityLeaderOnlyPoller;

@Singleton
public abstract class SingularityHistoryPersister extends SingularityLeaderOnlyPoller {

  private final SingularityConfiguration configuration;

  public SingularityHistoryPersister(SingularityConfiguration configuration) {
    super(configuration.getPersistHistoryEverySeconds(), TimeUnit.SECONDS);

    this.configuration = configuration;
  }

  @Override
  protected boolean abortsOnError() {
    return false;
  }

  @Override
  protected boolean isEnabled() {
    return configuration.getDatabaseConfiguration().isPresent();
  }

}
