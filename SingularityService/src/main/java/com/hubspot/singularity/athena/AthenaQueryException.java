package com.hubspot.singularity.athena;

public class AthenaQueryException extends Exception {
  public AthenaQueryException(String message, Exception cause) {
    super(message, cause);
  }

  public AthenaQueryException(String message) {
    super(message);
  }
}
