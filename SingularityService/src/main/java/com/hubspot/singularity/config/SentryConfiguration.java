package com.hubspot.singularity.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

public class SentryConfiguration {

  @NotEmpty
  @JsonProperty("dsn")
  private String dsn;

  @JsonProperty("prefix")
  private String prefix = "";

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
}
