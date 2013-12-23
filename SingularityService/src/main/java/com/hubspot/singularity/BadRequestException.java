package com.hubspot.singularity;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@SuppressWarnings("serial")
public class BadRequestException extends WebApplicationException {

  public BadRequestException(String message) {
    super(Response.status(Status.BAD_REQUEST).entity(message).type("text/plain").build());
  }
 
}
