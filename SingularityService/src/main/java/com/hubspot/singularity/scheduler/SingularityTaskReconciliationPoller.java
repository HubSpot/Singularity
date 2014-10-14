package com.hubspot.singularity.scheduler;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.mesos.SingularityMesosSchedulerDelegator;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

@Singleton
public class SingularityTaskReconciliationPoller extends SingularityLeaderOnlyPoller {

  private final SingularityTaskReconciliation taskReconciliation;

  @Inject
  public SingularityTaskReconciliationPoller(SingularityMesosSchedulerDelegator mesosScheduler, SingularityConfiguration configuration,
                                             SingularityAbort abort, SingularityExceptionNotifier exceptionNotifier, SingularityTaskReconciliation taskReconciliation) {
      super(mesosScheduler, exceptionNotifier, abort, configuration.getStartNewReconcileEverySeconds(), TimeUnit.SECONDS, SchedulerLockType.NO_LOCK);

    this.taskReconciliation = taskReconciliation;
  }

  @Override
  public void runActionOnPoll(SingularityMesosSchedulerDelegator mesosScheduler) {
       taskReconciliation.startReconciliation();
  }
}
