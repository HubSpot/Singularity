package com.hubspot.singularity.oomkiller;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;

public class SingularityOOMKillerMetrics {

  private final MetricRegistry registry;
  
  @Inject
  public SingularityOOMKillerMetrics(MetricRegistry registry) {
    this.registry = registry;
  }
  
  private String name(String... names) {
    return MetricRegistry.name(SingularityOOMKillerMetrics.class, names);
  }
  
  private void startJmxReporter() {
    JmxReporter reporter = JmxReporter.forRegistry(registry).build();
    reporter.start();
  }  
  
}
