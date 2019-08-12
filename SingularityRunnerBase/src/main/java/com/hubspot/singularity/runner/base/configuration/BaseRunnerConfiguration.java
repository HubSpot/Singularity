package com.hubspot.singularity.runner.base.configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class BaseRunnerConfiguration {
  public static final String DEFAULT_ROOT_LOG_LEVEL = "INFO";
  public static final String DEFAULT_HUBSPOT_LOG_LEVEL = "INFO";
  public static final String DEFAULT_DIRECTORY = "/var/log/singularity/";

  @JsonProperty
  private Optional<String> loggingFilename = Optional.empty();

  @JsonProperty
  private Optional<String> loggingDirectory = Optional.empty();

  @JsonProperty
  private Map<String, String> loggingLevel = new HashMap<>();

  @JsonProperty
  private Optional<String> loggingPattern = Optional.of("%-5level [%d] [%.15thread] %logger{50} %contextName - %msg%n");

  @JsonProperty
  private Optional<String> hostname = Optional.empty();

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
}
