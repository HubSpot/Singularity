package com.hubspot.singularity.scheduler;

import java.util.concurrent.TimeUnit;

import org.apache.mesos.SchedulerDriver;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityCloser;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.mesos.SingularityMesosSchedulerDelegator;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

public class SingularityCooldownPoller extends SingularityLeaderOnlyPoller {

  private final SingularityCooldownChecker checker;

  @Inject
  public SingularityCooldownPoller(SingularityExceptionNotifier exceptionNotifier, SingularityConfiguration configuration, SingularityCooldownChecker checker, SingularityAbort abort, SingularityCloser closer) {
    super(exceptionNotifier, abort, closer, TimeUnit.MINUTES.toMillis(configuration.getCooldownExpiresAfterMinutes()) / 2, TimeUnit.MILLISECONDS, SchedulerLockType.LOCK);

    this.checker = checker;
  }

  @Override
  public void runActionOnPoll(SingularityMesosSchedulerDelegator mesosScheduler, SchedulerDriver driver) {
    checker.checkCooldowns();
  }

}
