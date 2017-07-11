package com.hubspot.singularity.exceptions;

import javax.ws.rs.WebApplicationException;

public class DataCenterConflictException extends WebApplicationException {
  public DataCenterConflictException(String message) {
    super(message, 409);
  }
}
