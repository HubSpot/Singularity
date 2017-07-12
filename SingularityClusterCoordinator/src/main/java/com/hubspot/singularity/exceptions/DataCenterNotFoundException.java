package com.hubspot.singularity.exceptions;

import javax.ws.rs.WebApplicationException;

public class DataCenterNotFoundException extends WebApplicationException {
  public DataCenterNotFoundException(String message) {
    super(message, 500);
  }
}
