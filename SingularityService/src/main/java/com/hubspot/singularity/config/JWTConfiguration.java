package com.hubspot.singularity.config;

import javax.annotation.Nonnegative;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JWTConfiguration {
  @JsonProperty
  @NotNull
  private String authVerificationUrl = "";

  @JsonProperty
  @NotNull
  private String authCookieName = "";

  @JsonProperty
  @Nonnegative
  private long cacheValidationMs = 60000;

  public String getAuthVerificationUrl() {
    return authVerificationUrl;
  }

  public JWTConfiguration setAuthVerificationUrl(String authVerificationUrl) {
    this.authVerificationUrl = authVerificationUrl;
    return this;
  }

  public String getAuthCookieName() {
    return authCookieName;
  }

  public JWTConfiguration setAuthCookieName(String authCookieName) {
    this.authCookieName = authCookieName;
    return this;
  }

  public long getCacheValidationMs() {
    return cacheValidationMs;
  }

  public JWTConfiguration setCacheValidationMs(long cacheValidationMs) {
    this.cacheValidationMs = cacheValidationMs;
    return this;
  }
}
