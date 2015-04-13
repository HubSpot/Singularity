package com.hubspot.singularity.runner.base.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import io.dropwizard.configuration.ConfigurationValidationException;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.runner.base.configuration.BaseRunnerConfiguration;
import com.hubspot.singularity.runner.base.configuration.Configuration;
import com.hubspot.singularity.runner.base.configuration.SingularityRunnerBaseConfiguration;
import com.hubspot.singularity.runner.base.configuration.SingularityRunnerBaseLoggingConfiguration;

public class SingularityRunnerBaseLogging {
  private final ObjectMapper yamlMapper;
  private final Validator validator;
  private final SingularityRunnerBaseConfiguration baseConfiguration;
  private final SingularityRunnerBaseLoggingConfiguration loggingConfiguration;
  private final Set<BaseRunnerConfiguration> configurations;

  @Inject
  public SingularityRunnerBaseLogging(@Named(SingularityRunnerBaseModule.OBFUSCATED_YAML) ObjectMapper yamlMapper, Validator validator, SingularityRunnerBaseConfiguration baseConfiguration, SingularityRunnerBaseLoggingConfiguration loggingConfiguration, Set<BaseRunnerConfiguration> configurations) {
    this.yamlMapper = yamlMapper;
    this.validator = validator;
    this.loggingConfiguration = loggingConfiguration;
    this.configurations = configurations;
    this.baseConfiguration = baseConfiguration;

    printProperties(configureRootLogger());
  }

  public Optional<String> getRootLogPath() {
    if (loggingConfiguration.getFilename().isPresent()) {
      return Optional.of(Paths.get(loggingConfiguration.getDirectory().or(baseConfiguration.getLogging().getDirectory()).or(SingularityRunnerBaseLoggingConfiguration.DEFAULT_DIRECTORY)).resolve(loggingConfiguration.getFilename().get()).toString());
    } else {
      return Optional.absent();
    }
  }

  public void validateConfigurations() throws ConfigurationValidationException{
    for (BaseRunnerConfiguration config : configurations) {
      final Set<ConstraintViolation<BaseRunnerConfiguration>> violations = validator.validate(config);
      if (!violations.isEmpty()) {
        throw new ConfigurationValidationException(config.getClass().getSimpleName(), violations);
      }
    }

  }

  public void printProperties(Logger rootLogger) {
    for (BaseRunnerConfiguration configuration : configurations) {
      try {
        final Configuration annotation = configuration.getClass().getAnnotation(Configuration.class);
        final String filename = annotation == null ? "(unknown)" : annotation.value();
        rootLogger.info("Loaded {} from {}:\n{}", new String[]{configuration.getClass().getSimpleName(), filename, yamlMapper.writeValueAsString(configuration)});
      } catch (Exception e) {
        rootLogger.warn(String.format("Exception while attempting to print %s!", configuration.getClass().getName()), e);
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

    rootLogger.setLevel(Level.toLevel(loggingConfiguration.getRootLogLevel().or(baseConfiguration.getLogging().getRootLogLevel()).or(SingularityRunnerBaseLoggingConfiguration.DEFAULT_ROOT_LOG_LEVEL)));

    Logger hubSpotLogger = context.getLogger("com.hubspot");

    hubSpotLogger.setLevel(Level.toLevel(loggingConfiguration.getHubSpotLogLevel().or(baseConfiguration.getLogging().getHubSpotLogLevel()).or(SingularityRunnerBaseLoggingConfiguration.DEFAULT_HUBSPOT_LOG_LEVEL)));

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
    encoder.setPattern(loggingConfiguration.getLoggingPattern().or(baseConfiguration.getLogging().getLoggingPattern()).or(JavaUtils.LOGBACK_LOGGING_PATTERN));
    encoder.start();

    fileAppender.setEncoder(encoder);
    fileAppender.start();

    return fileAppender;
  }
}
