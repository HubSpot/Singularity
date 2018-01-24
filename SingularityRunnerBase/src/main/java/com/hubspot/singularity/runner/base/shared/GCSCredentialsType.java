package com.hubspot.singularity.runner.base.shared;

public enum  GCSCredentialsType {
  USER("authorized_user"), SERVICE_ACCOUNT("service_account");

  private final String type;

  GCSCredentialsType(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }
}
