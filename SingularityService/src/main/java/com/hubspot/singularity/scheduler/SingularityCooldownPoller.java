package com.hubspot.singularity.scheduler;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import com.google.inject.Inject;
import com.hubspot.singularity.config.SingularityConfiguration;

@Singleton
public class SingularityCooldownPoller extends SingularityLeaderOnlyPoller {

  private final SingularityCooldownChecker checker;

  @Inject
  SingularityCooldownPoller(SingularityConfiguration configuration, SingularityCooldownChecker checker) {
    super(TimeUnit.MINUTES.toMillis(configuration.getCooldownExpiresAfterMinutes()) / 2, TimeUnit.MILLISECONDS, true);

    this.checker = checker;
  }

  @Override
  public void runActionOnPoll() {
    checker.checkCooldowns();
  }
}
