package com.hubspot.singularity.executor.config;

import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.singularity.executor.SingularityExecutorProcessKiller;
import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseModule;

public class SingularityExecutorModule extends AbstractModule {

  public static final String RUNNER_TEMPLATE = "runner.sh";
  public static final String ENVIRONMENT_TEMPLATE = "deploy.env";
  
  @Override
  protected void configure() {
    install(new SingularityRunnerBaseModule("/etc/singularity.executor.properties", new SingularityExecutorConfigurationLoader()));
    
    bind(SingularityExecutorLogging.class).in(Scopes.SINGLETON);
    bind(SingularityTaskBuilder.class).in(Scopes.SINGLETON);
    bind(SingularityExecutorProcessKiller.class).in(Scopes.SINGLETON);
  }
  
  @Provides
  @Singleton
  @Named(RUNNER_TEMPLATE)
  public Mustache providesRunnerTemplate(MustacheFactory factory) {
    return factory.compile(RUNNER_TEMPLATE);
  }

  @Provides
  @Singleton
  @Named(ENVIRONMENT_TEMPLATE)
  public Mustache providesEnvironmentTemplate(MustacheFactory factory) {
    return factory.compile(ENVIRONMENT_TEMPLATE);
  }
  
}
