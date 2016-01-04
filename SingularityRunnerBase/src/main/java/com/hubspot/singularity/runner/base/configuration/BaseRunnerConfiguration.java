package com.hubspot.singularity.runner.base.configuration;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public abstract class BaseRunnerConfiguration {
  public static final String DEFAULT_ROOT_LOG_LEVEL = "INFO";
  public static final String DEFAULT_HUBSPOT_LOG_LEVEL = "INFO";
  public static final String DEFAULT_DIRECTORY = "/var/log/singularity/";

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
  private Optional<String> loggingPattern = Optional.of("%-5level [%d] [%.15thread] %logger{50} %contextName - %msg%n");

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
}
