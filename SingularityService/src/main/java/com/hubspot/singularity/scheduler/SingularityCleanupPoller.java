package com.hubspot.singularity.scheduler;

import java.util.concurrent.TimeUnit;

import org.apache.mesos.SchedulerDriver;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityCloser;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.mesos.SingularityMesosSchedulerDelegator;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

public class SingularityCleanupPoller extends SingularityLeaderOnlyPoller {

  private final SingularityCleaner cleaner;

  @Inject
  public SingularityCleanupPoller(SingularityExceptionNotifier exceptionNotifier, SingularityConfiguration configuration, SingularityCleaner cleaner, SingularityAbort abort, SingularityCloser closer) {
    super(exceptionNotifier, abort, closer, configuration.getCleanupEverySeconds(), TimeUnit.SECONDS,  SchedulerLockType.LOCK);

    this.cleaner = cleaner;
  }

  @Override
  public void runActionOnPoll(SingularityMesosSchedulerDelegator mesosScheduler, SchedulerDriver driver) {
    cleaner.drainCleanupQueue();
  }

}
