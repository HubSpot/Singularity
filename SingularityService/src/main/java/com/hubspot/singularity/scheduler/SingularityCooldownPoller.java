package com.hubspot.singularity.scheduler;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import javax.inject.Singleton;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.mesos.SingularityMesosModule;

@Singleton
public class SingularityCooldownPoller extends SingularityLeaderOnlyPoller {

  private final SingularityCooldownChecker checker;

  @Inject
  SingularityCooldownPoller(SingularityConfiguration configuration, SingularityCooldownChecker checker, @Named(SingularityMesosModule.SCHEDULER_LOCK_NAME) final Lock lock) {
    super(TimeUnit.MINUTES.toMillis(configuration.getCooldownExpiresAfterMinutes()) / 2, TimeUnit.MILLISECONDS, lock);

    this.checker = checker;
  }

  @Override
  public void runActionOnPoll() {
    checker.checkCooldowns();
  }
}
