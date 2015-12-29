package com.hubspot.singularity.runner.base.config;

import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.runner.base.configuration.BaseRunnerConfiguration;
import com.hubspot.singularity.runner.base.configuration.Configuration;
import com.hubspot.singularity.runner.base.configuration.SingularityRunnerBaseConfiguration;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;

@Singleton
public class SingularityRunnerBaseLogging {
  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(SingularityRunnerBaseLogging.class);

  // classes from these packages perform DEBUG logging before the loggers are properly configured...
  private static final String[] CHATTY_LOGGERS = {"org.jboss.logging", "org.hibernate", "com.github.jknack.handlebars"};

  private final ObjectMapper yamlMapper;
  private final SingularityRunnerBaseConfiguration baseConfiguration;
  private final BaseRunnerConfiguration primaryConfiguration;
  private final Set<BaseRunnerConfiguration> configurations;
  private final Optional<String> consolidatedConfigFilename;
  private final String executorPid;

  public static void quietEagerLogging() {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    for (String name : CHATTY_LOGGERS) {
      context.getLogger(name).setLevel(Level.WARN);
    }
  }

  @Inject
  public SingularityRunnerBaseLogging(@Named(SingularityRunnerBaseModule.OBFUSCATED_YAML) ObjectMapper yamlMapper, SingularityRunnerBaseConfiguration baseConfiguration,
      BaseRunnerConfiguration primaryConfiguration, Set<BaseRunnerConfiguration> configurations, @Named(SingularityRunnerBaseModule.CONSOLIDATED_CONFIG_FILENAME) Optional<String> consolidatedConfigFilename,
      @Named(SingularityRunnerBaseModule.PROCESS_NAME) String executorPid) {
    this.yamlMapper = yamlMapper;
    this.primaryConfiguration = primaryConfiguration;
    this.configurations = configurations;
    this.baseConfiguration = baseConfiguration;
    this.consolidatedConfigFilename = consolidatedConfigFilename;
    this.executorPid = executorPid;

    configureRootLogger();
    printProperties();
  }

  public Optional<String> getRootLogPath() {
    if (primaryConfiguration.getLoggingFilename().isPresent()) {
      return Optional.of(Paths.get(primaryConfiguration.getLoggingDirectory().or(baseConfiguration.getLoggingDirectory()).or(BaseRunnerConfiguration.DEFAULT_DIRECTORY)).resolve(primaryConfiguration.getLoggingFilename().get()).toString());
    } else {
      return Optional.absent();
    }
  }

  public void printProperties() {
    for (BaseRunnerConfiguration configuration : configurations) {
      try {
        final Configuration annotation = configuration.getClass().getAnnotation(Configuration.class);
        final String filename = consolidatedConfigFilename.or(annotation == null ? "(unknown)" : annotation.filename());
        LOG.info(String.format("Loaded %s from %s:%n%s", configuration.getClass().getSimpleName(), filename, yamlMapper.writeValueAsString(configuration)));
      } catch (Exception e) {
        LOG.warn(String.format("Exception while attempting to print %s!", configuration.getClass().getName()), e);
      }
    }
  }

  public Logger prepareRootLogger(LoggerContext context) {
    Logger rootLogger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

    rootLogger.detachAndStopAllAppenders();

    return rootLogger;
  }

  public Logger configureRootLogger() {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

    Logger rootLogger = prepareRootLogger(context);

    context.setName(executorPid);

    context.getLogger("ROOT").setLevel(Level.toLevel(BaseRunnerConfiguration.DEFAULT_ROOT_LOG_LEVEL));
    context.getLogger("com.hubspot").setLevel(Level.toLevel(BaseRunnerConfiguration.DEFAULT_HUBSPOT_LOG_LEVEL));

    for (Map.Entry<String, String> entry : baseConfiguration.getLoggingLevel().entrySet()) {
      context.getLogger(entry.getKey()).setLevel(Level.toLevel(entry.getValue()));
    }

    for (Map.Entry<String, String> entry : primaryConfiguration.getLoggingLevel().entrySet()) {
      context.getLogger(entry.getKey()).setLevel(Level.toLevel(entry.getValue()));
    }

    if (getRootLogPath().isPresent()) {
      rootLogger.addAppender(buildFileAppender(context, getRootLogPath().get()));
    }

    return rootLogger;
  }

  public FileAppender<ILoggingEvent> buildFileAppender(LoggerContext context, String file) {
    FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
    fileAppender.setFile(file);
    fileAppender.setContext(context);
    fileAppender.setPrudent(true);

    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
    encoder.setContext(context);
    encoder.setPattern(primaryConfiguration.getLoggingPattern().or(baseConfiguration.getLoggingPattern()).or(JavaUtils.LOGBACK_LOGGING_PATTERN));
    encoder.start();

    fileAppender.setEncoder(encoder);
    fileAppender.start();

    return fileAppender;
  }
}
