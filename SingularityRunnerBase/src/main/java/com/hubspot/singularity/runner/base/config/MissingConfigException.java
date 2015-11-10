package com.hubspot.singularity.runner.base.config;

public class MissingConfigException extends IllegalStateException {
  public MissingConfigException(Exception e) {
    super(e);
  }
}
