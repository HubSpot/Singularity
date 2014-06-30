package com.hubspot.singularity.config;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SentryConfiguration {

  @NotEmpty
  @JsonProperty("dsn")
  private String dsn;

  public String getDsn() {
    return dsn;
  }
}
