package com.hubspot.singularity.oomkiller;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;

public class SingularityOOMKillerMetrics {

  private final MetricRegistry registry;

  private final Meter oomHardKillsMeter;
  private final Meter eligibleForKillMeter;
  private final Meter oomSoftKillsMeter;
  private final Meter singularityFailuresMeter;
  private final Meter unknownExecutorsMeter;
  private final Meter singularityAlreadyKillingMeter;

  @Inject
  public SingularityOOMKillerMetrics(MetricRegistry registry) {
    this.registry = registry;

    this.eligibleForKillMeter = registry.meter(name("oomKiller", "eligibleForKill"));
    this.oomHardKillsMeter = registry.meter(name("oomKiller", "hardKills"));
    this.oomSoftKillsMeter = registry.meter(name("oomKiller", "softKills"));
    this.singularityFailuresMeter = registry.meter(name("oomKiller", "singularityFailures"));
    this.singularityAlreadyKillingMeter = registry.meter(name("oomKiller", "singularityAlreadyKilling"));
    this.unknownExecutorsMeter = registry.meter(name("oomKiller", "unknownExecutors"));

    startJmxReporter();
  }

  private String name(String... names) {
    return MetricRegistry.name(SingularityOOMKillerMetrics.class, names);
  }

  private void startJmxReporter() {
    JmxReporter reporter = JmxReporter.forRegistry(registry).build();
    reporter.start();
  }

  public Meter getEligibleForKillMeter() {
    return eligibleForKillMeter;
  }

  public Meter getSingularityAlreadyKillingMeter() {
    return singularityAlreadyKillingMeter;
  }

  public Meter getOomHardKillsMeter() {
    return oomHardKillsMeter;
  }

  public Meter getOomSoftKillsMeter() {
    return oomSoftKillsMeter;
  }

  public Meter getSingularityFailuresMeter() {
    return singularityFailuresMeter;
  }

  public Meter getUnknownExecutorsMeter() {
    return unknownExecutorsMeter;
  }

}
