package com.hubspot.singularity.scheduler;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import javax.inject.Singleton;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.mesos.SingularityMesosModule;

@Singleton
public class SingularityCleanupPoller extends SingularityLeaderOnlyPoller {

  private final SingularityCleaner cleaner;

  @Inject
  SingularityCleanupPoller(SingularityConfiguration configuration, SingularityCleaner cleaner, @Named(SingularityMesosModule.SCHEDULER_LOCK_NAME) final Lock lock) {
    super(configuration.getCleanupEverySeconds(), TimeUnit.SECONDS, lock);

    this.cleaner = cleaner;
  }

  @Override
  public void runActionOnPoll() {
    cleaner.drainCleanupQueue();
  }
}
