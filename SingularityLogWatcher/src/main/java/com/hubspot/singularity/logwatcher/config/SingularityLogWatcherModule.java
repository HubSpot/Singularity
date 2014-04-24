package com.hubspot.singularity.logwatcher.config;

import com.google.inject.AbstractModule;
import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseModule;

public class SingularityLogWatcherModule extends AbstractModule {

  @Override
  protected void configure() {
    install(new SingularityRunnerBaseModule("/etc/singularity.logwatcher.properties", new SingularityLogWatcherConfigurationLoader()));
  }
  
}
