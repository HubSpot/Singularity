package com.hubspot.singularity.exceptions;

@SuppressWarnings("serial")
public class InvalidSingularityTaskIdException extends RuntimeException {

  public InvalidSingularityTaskIdException(String message) {
    super(message);
  }

}
