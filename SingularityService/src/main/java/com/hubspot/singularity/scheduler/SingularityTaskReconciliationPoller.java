package com.hubspot.singularity.scheduler;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DisasterManager;

@Singleton
public class SingularityTaskReconciliationPoller extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityTaskReconciliationPoller.class);

  private final SingularityTaskReconciliation taskReconciliation;
  private final DisasterManager disasterManager;

  @Inject
  SingularityTaskReconciliationPoller(SingularityConfiguration configuration, SingularityTaskReconciliation taskReconciliation, DisasterManager disasterManager) {
    super(configuration.getStartNewReconcileEverySeconds(), TimeUnit.SECONDS);

    this.taskReconciliation = taskReconciliation;
    this.disasterManager = disasterManager;
  }

  @Override
  public void runActionOnPoll() {
    if (disasterManager.isDisabled(SingularityAction.TASK_RECONCILIATION)) {
      LOG.warn("Not starting implicit task reconciliation: {}", disasterManager.getDisabledAction(SingularityAction.TASK_RECONCILIATION).getMessage());
    } else {
      taskReconciliation.startReconciliation();
    }
  }

}
