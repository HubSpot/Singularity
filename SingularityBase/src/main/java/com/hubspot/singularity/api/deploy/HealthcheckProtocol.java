package com.hubspot.singularity.api.deploy;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public enum HealthcheckProtocol {

  HTTP("http"), HTTPS("https");

  private final String protocol;

  private HealthcheckProtocol(String protocol) {
    this.protocol = protocol;
  }

  public String getProtocol() {
    return protocol;
  }

}
