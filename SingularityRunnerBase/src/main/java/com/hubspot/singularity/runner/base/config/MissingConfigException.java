package com.hubspot.singularity.runner.base.config;

public class MissingConfigException extends RuntimeException {
  public MissingConfigException(String message) {
    super(message);
  }
}
