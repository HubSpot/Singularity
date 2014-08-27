package com.hubspot.singularity.client;

@SuppressWarnings("serial")
public class SingularityClientException extends RuntimeException {

  private int statusCode;

  public SingularityClientException() {
    super();
  }

  public SingularityClientException(String message) {
    super(message);
  }

  public SingularityClientException(String message, int statusCode) {
    super(message);
    this.statusCode = statusCode;
  }

  public SingularityClientException(String message, Throwable t) {
    super(message, t);
  }

  public int getStatusCode() {
    return statusCode;
  }
}
