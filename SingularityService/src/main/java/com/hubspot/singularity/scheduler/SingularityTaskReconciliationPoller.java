package com.hubspot.singularity.scheduler;

import java.util.concurrent.TimeUnit;

import org.apache.mesos.SchedulerDriver;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityCloser;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.mesos.SingularityMesosSchedulerDelegator;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

public class SingularityTaskReconciliationPoller extends SingularityLeaderOnlyPoller {

  private final SingularityTaskReconciliation taskReconciliation;

  @Inject
  public SingularityTaskReconciliationPoller(SingularityConfiguration configuration, SingularityAbort abort, SingularityCloser closer, SingularityExceptionNotifier exceptionNotifier,
      SingularityTaskReconciliation taskReconciliation) {
    super(exceptionNotifier, abort, closer, configuration.getReconcileTasksEveryMillis(), TimeUnit.MILLISECONDS, SchedulerLockType.NO_LOCK);

    this.taskReconciliation = taskReconciliation;
  }

  @Override
  public void runActionOnPoll(SingularityMesosSchedulerDelegator mesosScheduler, SchedulerDriver driver) {
    taskReconciliation.reconcileTasks(driver);
  }

}
