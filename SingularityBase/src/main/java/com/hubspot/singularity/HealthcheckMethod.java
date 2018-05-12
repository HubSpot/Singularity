package com.hubspot.singularity;

public enum  HealthcheckMethod {

  GET("GET"), POST("POST");

  private String method;

  private HealthcheckMethod(String method) {
    this.method = method;
  }

  public String getMethod() {
    return method;
  }
}
