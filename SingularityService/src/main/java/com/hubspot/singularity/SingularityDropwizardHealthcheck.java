package com.hubspot.singularity;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;

import org.apache.curator.framework.recipes.leader.LeaderLatch;

import com.codahale.metrics.health.HealthCheck;
import com.hubspot.singularity.mesos.SingularityMesosScheduler;

public class SingularityDropwizardHealthcheck extends HealthCheck {

  private final SingularityMesosScheduler mesosScheduler;
  private final LeaderLatch leaderLatch;

  @Inject
  public SingularityDropwizardHealthcheck(final SingularityMesosScheduler mesosScheduler, final LeaderLatch leaderLatch) {
    this.mesosScheduler = checkNotNull(mesosScheduler, "mesosScheduler is null");
    this.leaderLatch = checkNotNull(leaderLatch, "leaderLatch is null");
  }

  @Override
  protected Result check() throws Exception {
    if (leaderLatch.hasLeadership()) {
      return mesosScheduler.isRunning() ? Result.healthy("Leading and scheduler is running.") : Result.unhealthy("Leading, but scheduler is not running!");
    } else {
      return mesosScheduler.isRunning() ? Result.unhealthy("Not leading, but scheduler is running!") : Result.healthy("Not leading.");
    }
  }
}
