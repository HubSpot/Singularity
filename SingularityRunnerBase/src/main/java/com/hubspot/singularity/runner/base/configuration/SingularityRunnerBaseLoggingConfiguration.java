package com.hubspot.singularity.runner.base.configuration;

import java.util.Properties;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.hubspot.mesos.JavaUtils;

public class SingularityRunnerBaseLoggingConfiguration implements OverridableByProperty {
  public static final String DEFAULT_ROOT_LOG_LEVEL = "INFO";
  public static final String DEFAULT_HUBSPOT_LOG_LEVEL = "INFO";
  public static final String DEFAULT_DIRECTORY = "/var/log/singularity/";

  public static final String LOGGING_PATTERN = "logging.pattern";
  public static final String ROOT_LOG_NAME = "root.log.name";

  public static final String ROOT_LOG_LEVEL = "root.log.level";
  public static final String HUBSPOT_LOG_LEVEL = "hubspot.log.level";

  public static SingularityRunnerBaseLoggingConfiguration defaultBaseConfig() {
    final SingularityRunnerBaseLoggingConfiguration config = new SingularityRunnerBaseLoggingConfiguration();

    config.setDirectory(Optional.of(DEFAULT_DIRECTORY));
    config.setRootLogLevel(Optional.of("INFO"));
    config.setHubSpotLogLevel(Optional.of(HUBSPOT_LOG_LEVEL));
    config.setLoggingPattern(Optional.of(JavaUtils.LOGBACK_LOGGING_PATTERN));

    return config;
  }

  @JsonProperty
  @NotNull
  private Optional<String> filename = Optional.absent();

  @JsonProperty
  @NotNull
  private Optional<String> directory = Optional.absent();

  @JsonProperty
  @NotNull
  private Optional<String> rootLogLevel = Optional.absent();

  @JsonProperty
  @NotNull
  private Optional<String> hubSpotLogLevel = Optional.absent();

  @JsonProperty
  @NotNull
  private Optional<String> loggingPattern = Optional.absent();

  public Optional<String> getFilename() {
    return filename;
  }

  public void setFilename(Optional<String> filename) {
    this.filename = filename;
  }

  public Optional<String> getDirectory() {
    return directory;
  }

  public void setDirectory(Optional<String> directory) {
    this.directory = directory;
  }

  public Optional<String> getRootLogLevel() {
    return rootLogLevel;
  }

  public void setRootLogLevel(Optional<String> rootLogLevel) {
    this.rootLogLevel = rootLogLevel;
  }

  public Optional<String> getHubSpotLogLevel() {
    return hubSpotLogLevel;
  }

  public void setHubSpotLogLevel(Optional<String> hubSpotLogLevel) {
    this.hubSpotLogLevel = hubSpotLogLevel;
  }

  public Optional<String> getLoggingPattern() {
    return loggingPattern;
  }

  public void setLoggingPattern(Optional<String> loggingPattern) {
    this.loggingPattern = loggingPattern;
  }

  @Override
  public void updateFromProperties(Properties properties) {
    if (properties.containsKey(SingularityRunnerBaseConfiguration.ROOT_LOG_DIRECTORY) && !Strings.isNullOrEmpty(properties.getProperty(SingularityRunnerBaseConfiguration.ROOT_LOG_DIRECTORY))) {
      setDirectory(Optional.of(properties.getProperty(SingularityRunnerBaseConfiguration.ROOT_LOG_DIRECTORY)));
    }
    if (properties.containsKey(LOGGING_PATTERN) && !Strings.isNullOrEmpty(properties.getProperty(LOGGING_PATTERN))) {
      setLoggingPattern(Optional.of(properties.getProperty(LOGGING_PATTERN)));
    }

    if (properties.containsKey(ROOT_LOG_NAME) && !Strings.isNullOrEmpty(properties.getProperty(ROOT_LOG_NAME))) {
      setFilename(Optional.of(properties.getProperty(ROOT_LOG_NAME)));
    }

    if (properties.containsKey(ROOT_LOG_LEVEL) && !Strings.isNullOrEmpty(properties.getProperty(ROOT_LOG_LEVEL))) {
      setRootLogLevel(Optional.of(properties.getProperty(ROOT_LOG_LEVEL)));
    }

    if (properties.containsKey(HUBSPOT_LOG_LEVEL) && !Strings.isNullOrEmpty(properties.getProperty(HUBSPOT_LOG_LEVEL))) {
      setHubSpotLogLevel(Optional.of(properties.getProperty(HUBSPOT_LOG_LEVEL)));
    }
  }


}
