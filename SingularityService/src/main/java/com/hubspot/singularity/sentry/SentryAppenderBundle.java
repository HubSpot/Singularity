package com.hubspot.singularity.sentry;

import net.kencochrane.raven.Raven;
import net.kencochrane.raven.RavenFactory;
import net.kencochrane.raven.logback.SentryAppender;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

import com.google.common.base.Optional;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.config.SentryConfiguration;

import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class SentryAppenderBundle implements ConfiguredBundle<SingularityConfiguration> {

  private final static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(SentryAppenderBundle.class);

  @Override
  public void run(SingularityConfiguration configuration, Environment environment) throws Exception {
    Optional<SentryConfiguration> maybeSentryConfiguration = configuration.getSentryConfiguration();

    if (!maybeSentryConfiguration.isPresent() || !maybeSentryConfiguration.get().getDsn().isPresent()) {
      LOG.warn("SentryAppenderBundle is installed, sentry configuration is missing");
      return;
    }
    
    String dsn = maybeSentryConfiguration.get().getDsn().get();
    Raven raven = RavenFactory.ravenInstance(dsn);
    SentryAppender sentryAppender = new SentryAppender(raven);

    Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    sentryAppender.start();

    rootLogger.addAppender(sentryAppender);
  }

  public void initialize(Bootstrap<?> bootstrap) {
  }
}
