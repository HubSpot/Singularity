package com.hubspot.singularity.config;

import javax.validation.constraints.NotNull;

import com.google.common.base.Optional;

public class SentryConfiguration {

  @NotNull
  private String dsn;

  @NotNull
  private String level;

  public Optional<String> getDsn() {
    return Optional.fromNullable(dsn);
  }
}
