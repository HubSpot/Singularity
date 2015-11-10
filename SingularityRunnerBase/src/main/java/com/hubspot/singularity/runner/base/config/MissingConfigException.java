package com.hubspot.singularity.runner.base.config;

public class MissingConfigException extends IllegalStateException {
  public MissingConfigException(String message) {
    super(message);
  }
}
