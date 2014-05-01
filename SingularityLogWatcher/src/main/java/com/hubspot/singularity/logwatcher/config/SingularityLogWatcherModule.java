package com.hubspot.singularity.logwatcher.config;

import java.util.List;

import org.fluentd.logger.FluentLogger;

import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.singularity.logwatcher.config.SingularityLogWatcherConfiguration.FluentdHost;
import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseModule;

public class SingularityLogWatcherModule extends AbstractModule {
  
  public static final String LOGROTATE_TEMPLATE = "logrotate.template";
  
  @Override
  protected void configure() {
    install(new SingularityRunnerBaseModule("/etc/singularity.logwatcher.properties", new SingularityLogWatcherConfigurationLoader()));
  }
  
  @Provides
  @Singleton
  public List<FluentLogger> getFluentLoggers(SingularityLogWatcherConfiguration configuration) {
    final List<FluentLogger> loggers = Lists.newArrayListWithCapacity(configuration.getFluentdHosts().size());
    for (FluentdHost host : configuration.getFluentdHosts()) {
      loggers.add(FluentLogger.getLogger(null, host.getHost(), host.getPort()));
    }
    return loggers;
  }

  @Provides
  @Singleton
  @Named(LOGROTATE_TEMPLATE)
  public Mustache providesEnvironmentTemplate(MustacheFactory factory) {
    return factory.compile(LOGROTATE_TEMPLATE);
  }
  
}
