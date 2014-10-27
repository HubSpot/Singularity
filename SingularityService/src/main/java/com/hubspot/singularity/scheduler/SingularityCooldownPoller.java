package com.hubspot.singularity.scheduler;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.apache.curator.framework.recipes.leader.LeaderLatch;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.mesos.SingularityMesosSchedulerDelegator;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

@Singleton
public class SingularityCooldownPoller extends SingularityLeaderOnlyPoller {

  private final SingularityCooldownChecker checker;

  @Inject
  public SingularityCooldownPoller(LeaderLatch leaderLatch, SingularityMesosSchedulerDelegator mesosScheduler, SingularityExceptionNotifier exceptionNotifier, SingularityConfiguration configuration, SingularityCooldownChecker checker, SingularityAbort abort) {
      super(leaderLatch, mesosScheduler, exceptionNotifier, abort, TimeUnit.MINUTES.toMillis(configuration.getCooldownExpiresAfterMinutes()) / 2, TimeUnit.MILLISECONDS, SchedulerLockType.LOCK);

    this.checker = checker;
  }

  @Override
  public void runActionOnPoll() {
    checker.checkCooldowns();
  }
}
