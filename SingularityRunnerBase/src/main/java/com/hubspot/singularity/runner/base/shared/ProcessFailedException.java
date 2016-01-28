package com.hubspot.singularity.runner.base.shared;

public class ProcessFailedException extends Exception {

  private static final long serialVersionUID = 1L;

  public ProcessFailedException(String message) {
    super(message);
  }

  public ProcessFailedException(String message, Throwable cause) {
    super(message, cause);
  }

}
