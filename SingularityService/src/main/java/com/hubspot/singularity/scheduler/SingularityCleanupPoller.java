package com.hubspot.singularity.scheduler;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.mesos.SingularityMesosSchedulerDelegator;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

@Singleton
public class SingularityCleanupPoller extends SingularityLeaderOnlyPoller {

  private final SingularityCleaner cleaner;

  @Inject
  public SingularityCleanupPoller(SingularityMesosSchedulerDelegator mesosScheduler, SingularityExceptionNotifier exceptionNotifier, SingularityConfiguration configuration,
      SingularityCleaner cleaner, SingularityAbort abort) {
    super(mesosScheduler, exceptionNotifier, abort, configuration.getCleanupEverySeconds(), TimeUnit.SECONDS,  SchedulerLockType.LOCK);

    this.cleaner = cleaner;
  }

  @Override
  public void runActionOnPoll(SingularityMesosSchedulerDelegator mesosScheduler) {
    cleaner.drainCleanupQueue();
  }
}
