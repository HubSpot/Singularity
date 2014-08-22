package com.hubspot.singularity;

@SuppressWarnings("serial")
public class InvalidSingularityTaskIdException extends RuntimeException {

  public InvalidSingularityTaskIdException(String message) {
    super(message);
  }

}
