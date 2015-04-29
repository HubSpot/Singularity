package com.hubspot.singularity.logwatcher.config;

import java.util.List;

import org.fluentd.logger.FluentLogger;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.hubspot.singularity.logwatcher.config.SingularityLogWatcherConfiguration.FluentdHost;
import com.hubspot.singularity.logwatcher.driver.SingularityLogWatcherDriver;
import com.hubspot.singularity.runner.base.shared.SingularityDriver;

public class SingularityLogWatcherModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(SingularityDriver.class).to(SingularityLogWatcherDriver.class);
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
}
