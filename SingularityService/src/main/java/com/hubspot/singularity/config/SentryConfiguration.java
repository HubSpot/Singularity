package com.hubspot.singularity.config;

import org.hibernate.validator.constraints.NotEmpty;

import java.util.Arrays;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SentryConfiguration {

  @NotEmpty
  @JsonProperty("dsn")
  private String dsn;

  @JsonProperty("prefix")
  private String prefix = "";

  @JsonProperty
  @NotNull
  private List<String> ignoredTraceSignatures = Arrays.asList("ch.qos.logback", "org.log4j", "sun.reflect");

  @JsonProperty
  @NotNull
  private List<String> singularityTraceSignatures = Arrays.asList("com.hubspot");

  public String getDsn() {
    return dsn;
  }

  public void setDsn(String dsn) {
    this.dsn = dsn;
  }

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public List<String> getIgnoredTraceSignatures() {
    return ignoredTraceSignatures;
  }

  public void setIgnoredTraceSignatures(List<String> ignoredTraceSignatures) {
    this.ignoredTraceSignatures = ignoredTraceSignatures;
  }

  public List<String> getSingularityTraceSignatures() {
    return singularityTraceSignatures;
  }

  public void setSingularityTraceSignatures(List<String> singularityTraceSignatures) {
    this.singularityTraceSignatures = singularityTraceSignatures;
  }
}
