package com.hubspot.singularity;

import javax.inject.Inject;

import com.codahale.metrics.health.HealthCheck;
import com.hubspot.singularity.mesos.SingularityMesosSchedulerDelegator;

public class SingularityDropwizardHealthcheck extends HealthCheck
{
  private final SingularityMesosSchedulerDelegator mesosScheduler;

  @Inject
  public SingularityDropwizardHealthcheck(final SingularityMesosSchedulerDelegator mesosScheduler) {
    this.mesosScheduler = mesosScheduler;
  }

  @Override
  protected Result check() throws Exception {
    return mesosScheduler.isRunning() ? Result.healthy() : Result.unhealthy("scheduler not running");
  }
}
