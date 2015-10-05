package com.hubspot.singularity.guice;

import static com.google.common.base.Preconditions.checkNotNull;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.google.inject.Provider;

import io.dropwizard.setup.Environment;

public class DropwizardMetricRegistryProvider implements Provider<MetricRegistry> {
  private final Environment environment;

  @Inject
  public DropwizardMetricRegistryProvider(Environment environment) {
    this.environment = checkNotNull(environment, "environment is null");
  }

  @Override
  public MetricRegistry get() {
    return environment.metrics();
  }
}
