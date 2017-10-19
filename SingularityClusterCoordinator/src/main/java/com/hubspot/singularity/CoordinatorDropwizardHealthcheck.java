package com.hubspot.singularity;

import com.codahale.metrics.health.HealthCheck;
import com.google.inject.Inject;
import com.hubspot.singularity.config.ClusterCoordinatorConfiguration;

public class CoordinatorDropwizardHealthcheck extends HealthCheck {
  private final ClusterCoordinatorConfiguration configuration;

  @Inject
  public CoordinatorDropwizardHealthcheck(ClusterCoordinatorConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  protected Result check() throws Exception {
    if (configuration.getDataCenters().isEmpty()) {
      return Result.unhealthy("No configured data centers");
    } else {
      return Result.healthy("Valid data centers", configuration.getDataCenters());
    }
  }
}
