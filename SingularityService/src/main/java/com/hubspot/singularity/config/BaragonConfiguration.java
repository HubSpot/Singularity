package com.hubspot.singularity.config;

import com.google.common.base.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BaragonConfiguration {
  @JsonIgnoreProperties(ignoreUnknown = true)

  @JsonProperty("baseUri")
  private String baseUri;

  @JsonProperty("authkey")
  private Optional<String> authkey;

  @JsonProperty("requestTimeoutMs")
  private int requestTimeoutMs = 2000;

  public String getBaseUri() {
    return baseUri;
  }

  public void setBaseUri(String baseUri) {
    this.baseUri = baseUri;
  }

  public Optional<String> getAuthkey() {
    return authkey;
  }

  public void setAuthkey(Optional<String> authkey) {
    this.authkey = authkey;
  }

  public int getRequestTimeoutMs() {
    return requestTimeoutMs;
  }

  public void setRequestTimeoutMs(int requestTimeoutMs) {
    this.requestTimeoutMs = requestTimeoutMs;
  }
}
