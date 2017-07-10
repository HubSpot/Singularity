package com.hubspot.singularity.exceptions;

import javax.ws.rs.WebApplicationException;

public class NotImplemenedException extends WebApplicationException {
  public NotImplemenedException() {
    super("Not Implemented", 500);
  }
}
