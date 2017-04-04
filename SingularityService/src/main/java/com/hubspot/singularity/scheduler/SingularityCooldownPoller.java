package com.hubspot.singularity.scheduler;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import com.google.inject.Inject;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.mesos.SingularitySchedulerLock;

@Singleton
public class SingularityCooldownPoller extends SingularityLeaderOnlyPoller {

  private final SingularityCooldownChecker checker;

  @Inject
  SingularityCooldownPoller(SingularityConfiguration configuration, SingularityCooldownChecker checker, SingularitySchedulerLock lock) {
    super(TimeUnit.MINUTES.toMillis(configuration.getCooldownExpiresAfterMinutes()) / 2, TimeUnit.MILLISECONDS, lock);

    this.checker = checker;
  }

  @Override
  public void runActionOnPoll() {
    checker.checkCooldowns();
  }
}
