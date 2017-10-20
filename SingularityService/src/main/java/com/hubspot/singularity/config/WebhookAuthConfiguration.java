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

  @JsonProperty
  @NotNull
  private String defaultEmailDomain = "";

  public String getAuthVerificationUrl() {
    return authVerificationUrl;
  }

  public WebhookAuthConfiguration setAuthVerificationUrl(String authVerificationUrl) {
    this.authVerificationUrl = authVerificationUrl;
    return this;
  }

  public long getCacheValidationMs() {
    return cacheValidationMs;
  }

  public WebhookAuthConfiguration setCacheValidationMs(long cacheValidationMs) {
    this.cacheValidationMs = cacheValidationMs;
    return this;
  }

  public String getDefaultEmailDomain() {
    return defaultEmailDomain;
  }

  public WebhookAuthConfiguration setDefaultEmailDomain(String defaultEmailDomain) {
    this.defaultEmailDomain = defaultEmailDomain;
    return this;
  }
}
