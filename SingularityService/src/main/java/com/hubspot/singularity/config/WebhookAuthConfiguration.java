package com.hubspot.singularity.config;

import javax.annotation.Nonnegative;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WebhookAuthConfiguration {
  @JsonProperty
  @NotNull
  private String authVerificationUrl = "";

  @JsonProperty
  @Nonnegative
  private long cacheValidationMs = 60000;

  public String getAuthVerificationUrl() {
    return authVerificationUrl;
  }

  public void setAuthVerificationUrl(String authVerificationUrl) {
    this.authVerificationUrl = authVerificationUrl;
  }

  public long getCacheValidationMs() {
    return cacheValidationMs;
  }

  public void setCacheValidationMs(long cacheValidationMs) {
    this.cacheValidationMs = cacheValidationMs;
  }
}
