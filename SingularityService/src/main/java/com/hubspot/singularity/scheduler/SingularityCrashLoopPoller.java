package com.hubspot.singularity.scheduler;

import com.google.inject.Inject;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;

@Singleton
public class SingularityCrashLoopPoller extends SingularityLeaderOnlyPoller {
  private final SingularityCrashLoopChecker checker;

  @Inject
  SingularityCrashLoopPoller(SingularityCrashLoopChecker checker) {
    super(1, TimeUnit.MINUTES);
    this.checker = checker;
  }

  @Override
  public void runActionOnPoll() {
    checker.checkCooldowns();
  }
}
