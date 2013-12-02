package com.hubspot.singularity.config;

import java.util.TimeZone;

import javax.validation.constraints.NotNull;

import ch.qos.logback.classic.Level;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SMTPLoggingConfiguration {

  @NotNull
  @JsonProperty
  protected boolean enabled = false;

  @NotNull
  @JsonProperty
  protected Level threshold = Level.ERROR;

  @JsonProperty
  protected String logFormat;

  @NotNull
  @JsonProperty
  protected TimeZone timeZone = TimeZone.getTimeZone("UTC");

  @NotNull
  @JsonProperty
  protected String subject = "%logger{20} - %m";

  @JsonProperty
  protected String localhost;

  @NotNull
  @JsonProperty
  protected String charsetEncoding = "UTF-8";

  public String getCharsetEncoding() {
    return charsetEncoding;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public Level getThreshold() {
    return threshold;
  }

  public TimeZone getTimeZone() {
    return timeZone;
  }

  public Optional<String> getLogFormat() {
    return Optional.fromNullable(logFormat);
  }

  public Optional<String> getLocalhost() {
    return Optional.fromNullable(localhost);
  }
  
  public String getSubject() {
    return subject;
  }

}
