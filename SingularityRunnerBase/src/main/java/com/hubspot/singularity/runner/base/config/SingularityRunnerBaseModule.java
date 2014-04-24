package com.hubspot.singularity.runner.base.config;

import com.google.inject.AbstractModule;

public class SingularityRunnerBaseModule extends AbstractModule {

  private final String rootPath;
  private final SingularityRunnerBaseConfigurationLoader configuration;
  
  public SingularityRunnerBaseModule(String rootPath, SingularityRunnerBaseConfigurationLoader configuration) {
    this.rootPath = rootPath;
    this.configuration = configuration;
  }

  @Override
  protected void configure() {
    configuration.bindPropertiesFile(rootPath, binder());
    
    bind(SingularityRunnerBaseLogging.class).asEagerSingleton();
  }

}
