package com.hubspot.singularity.scheduler;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import com.google.inject.Inject;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.mesos.SingularitySchedulerLock;

@Singleton
public class SingularityCleanupPoller extends SingularityLeaderOnlyPoller {

  private final SingularityCleaner cleaner;

  @Inject
  SingularityCleanupPoller(SingularityConfiguration configuration, SingularityCleaner cleaner, SingularitySchedulerLock lock) {
    super(configuration.getCleanupEverySeconds(), TimeUnit.SECONDS, lock);

    this.cleaner = cleaner;
  }

  @Override
  public void runActionOnPoll() {
    cleaner.drainCleanupQueue();
  }
}
