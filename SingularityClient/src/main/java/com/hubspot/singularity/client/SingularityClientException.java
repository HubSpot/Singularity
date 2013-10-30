package com.hubspot.singularity.client;

@SuppressWarnings("serial")
public class SingularityClientException extends RuntimeException {

  public SingularityClientException() {
    super();
  }

  public SingularityClientException(String message) {
    super(message);
  }
  
  public SingularityClientException(String message, Throwable t) {
    super(message, t);
  }

}
