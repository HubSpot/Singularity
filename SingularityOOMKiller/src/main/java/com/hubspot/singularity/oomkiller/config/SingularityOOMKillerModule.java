package com.hubspot.singularity.oomkiller.config;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.hubspot.singularity.oomkiller.SingularityOOMKillerDriver;
import com.hubspot.singularity.oomkiller.SingularityOOMKillerMetrics;
import com.hubspot.singularity.runner.base.shared.SingularityDriver;

public class SingularityOOMKillerModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(SingularityDriver.class).to(SingularityOOMKillerDriver.class);
    bind(SingularityOOMKillerMetrics.class).in(Scopes.SINGLETON);
  }
}
