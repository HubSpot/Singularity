package com.hubspot.singularity;

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
