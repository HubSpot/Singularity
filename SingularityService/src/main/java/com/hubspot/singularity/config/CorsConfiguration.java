package com.hubspot.singularity.config;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CorsConfiguration {

  @JsonProperty
  private boolean enabled = false;

  @JsonProperty
  @NotNull
  private String allowedHeaders = "X-Requested-With,Content-Type,Accept,Origin,Authorization";

  @JsonProperty
  @NotNull
  private String allowedMethods = "OPTIONS,GET,PUT,POST,DELETE,HEAD";

  @JsonProperty
  @NotNull
  private String allowedOrigins = "*";

  @JsonProperty
  private boolean allowCredentials = true;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getAllowedHeaders() {
    return allowedHeaders;
  }

  public void setAllowedHeaders(String allowedHeaders) {
    this.allowedHeaders = allowedHeaders;
  }

  public String getAllowedMethods() {
    return allowedMethods;
  }

  public void setAllowedMethods(String allowedMethods) {
    this.allowedMethods = allowedMethods;
  }

  public String getAllowedOrigins() {
    return allowedOrigins;
  }

  public void setAllowedOrigins(String allowedOrigins) {
    this.allowedOrigins = allowedOrigins;
  }

  public boolean isAllowCredentials() {
    return allowCredentials;
  }

  public void setAllowCredentials(boolean allowCredentials) {
    this.allowCredentials = allowCredentials;
  }
}
