package com.hubspot.singularity.runner.base.configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.base.Strings;

public abstract class BaseRunnerConfiguration implements OverridableByProperty {
  public static final String DEFAULT_ROOT_LOG_LEVEL = "INFO";
  public static final String DEFAULT_HUBSPOT_LOG_LEVEL = "INFO";
  public static final String DEFAULT_DIRECTORY = "/var/log/singularity/";

  public static final String LOGGING_PATTERN = "logging.pattern";
  public static final String ROOT_LOG_NAME = "root.log.name";

  public static final String ROOT_LOG_LEVEL = "root.log.level";
  public static final String HUBSPOT_LOG_LEVEL = "hubspot.log.level";

  @NotNull
  @JsonProperty
  private Optional<String> loggingFilename = Optional.absent();

  @NotNull
  @JsonProperty
  private Optional<String> loggingDirectory = Optional.absent();

  @NotNull
  @JsonProperty
  private Map<String, String> loggingLevel = new HashMap<>();

  @NotNull
  @JsonProperty
  private Optional<String> loggingPattern = Optional.absent();

  @NotNull
  @JsonProperty
  private Optional<String> hostname = Optional.absent();

  protected BaseRunnerConfiguration(Optional<String> loggingFilename) {
    this.loggingFilename = loggingFilename;
  }

  public Optional<String> getHostname() {
    return hostname;
  }

  public void setHostname(Optional<String> hostname) {
    this.hostname = hostname;
  }

  public Optional<String> getLoggingFilename() {
    return loggingFilename;
  }

  public void setLoggingFilename(Optional<String> loggingFilename) {
    this.loggingFilename = loggingFilename;
  }

  public Optional<String> getLoggingDirectory() {
    return loggingDirectory;
  }

  public void setLoggingDirectory(Optional<String> loggingDirectory) {
    this.loggingDirectory = loggingDirectory;
  }

  public Map<String, String> getLoggingLevel() {
    return loggingLevel;
  }

  public void setLoggingLevel(Map<String, String> loggingLevel) {
    this.loggingLevel = loggingLevel;
  }

  public Optional<String> getLoggingPattern() {
    return loggingPattern;
  }

  public void setLoggingPattern(Optional<String> loggingPattern) {
    this.loggingPattern = loggingPattern;
  }

  public void updateLoggingFromProperties(Properties properties) {
    if (properties.containsKey(SingularityRunnerBaseConfiguration.ROOT_LOG_DIRECTORY) && !Strings.isNullOrEmpty(properties.getProperty(SingularityRunnerBaseConfiguration.ROOT_LOG_DIRECTORY))) {
      setLoggingDirectory(Optional.of(properties.getProperty(SingularityRunnerBaseConfiguration.ROOT_LOG_DIRECTORY)));
    }
    if (properties.containsKey(LOGGING_PATTERN) && !Strings.isNullOrEmpty(properties.getProperty(LOGGING_PATTERN))) {
      setLoggingPattern(Optional.of(properties.getProperty(LOGGING_PATTERN)));
    }

    if (properties.containsKey(ROOT_LOG_NAME) && !Strings.isNullOrEmpty(properties.getProperty(ROOT_LOG_NAME))) {
      setLoggingFilename(Optional.of(properties.getProperty(ROOT_LOG_NAME)));
    }

    if (properties.containsKey(ROOT_LOG_LEVEL) && !Strings.isNullOrEmpty(properties.getProperty(ROOT_LOG_LEVEL))) {
      getLoggingLevel().put("ROOT", properties.getProperty(ROOT_LOG_LEVEL));
    }

    if (properties.containsKey(HUBSPOT_LOG_LEVEL) && !Strings.isNullOrEmpty(properties.getProperty(HUBSPOT_LOG_LEVEL))) {
      getLoggingLevel().put("com.hubspot", properties.getProperty(HUBSPOT_LOG_LEVEL));
    }
  }
}
