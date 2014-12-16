package com.hubspot.singularity.scheduler;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import com.google.inject.Inject;
import com.hubspot.singularity.config.SingularityConfiguration;

@Singleton
public class SingularityTaskReconciliationPoller extends SingularityLeaderOnlyPoller {

  private final SingularityTaskReconciliation taskReconciliation;

  @Inject
  SingularityTaskReconciliationPoller(SingularityConfiguration configuration, SingularityTaskReconciliation taskReconciliation) {
    super(configuration.getStartNewReconcileEverySeconds(), TimeUnit.SECONDS);

    this.taskReconciliation = taskReconciliation;
  }

  @Override
  public void runActionOnPoll() {
    taskReconciliation.startReconciliation();
  }

}
