package com.hubspot.singularity.executor.config;

import java.io.IOException;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.singularity.executor.SingularityExecutorProcessKiller;
import com.hubspot.singularity.executor.handlebars.BashEscapedHelper;
import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseModule;

public class SingularityExecutorModule extends AbstractModule {

  public static final String RUNNER_TEMPLATE = "runner.sh";
  public static final String ENVIRONMENT_TEMPLATE = "deploy.env";
  public static final String LOGROTATE_TEMPLATE = "logrotate.conf";
  
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
  public Template providesRunnerTemplate(Handlebars handlebars) throws IOException {
    return handlebars.compile(RUNNER_TEMPLATE);
  }

  @Provides
  @Singleton
  @Named(ENVIRONMENT_TEMPLATE)
  public Template providesEnvironmentTemplate(Handlebars handlebars) throws IOException {
    return handlebars.compile(ENVIRONMENT_TEMPLATE);
  }
  
  @Provides
  @Singleton
  @Named(LOGROTATE_TEMPLATE)
  public Template providesLogrotateTemplate(Handlebars handlebars) throws IOException {
    return handlebars.compile(LOGROTATE_TEMPLATE);
  }

  @Provides
  @Singleton
  public Handlebars providesHandlebars() {
    final Handlebars handlebars = new Handlebars();

    handlebars.registerHelper("bashEscaped", new BashEscapedHelper());

    return handlebars;
  }
  
}
