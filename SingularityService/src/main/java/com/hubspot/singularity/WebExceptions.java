package com.hubspot.singularity;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class WebExceptions {

  public static WebApplicationException badRequest(String message, Object... args) {
    throw webException(Status.BAD_REQUEST.getStatusCode(), message, args);
  }

  public static WebApplicationException timeout(String message, Object... args) {
    throw webException(408, message, args);
  }

  public static WebApplicationException conflict(String message, Object... args) {
    throw webException(Status.CONFLICT.getStatusCode(), message, args);
  }

  public static WebApplicationException notFound(String message, Object...args) {
    throw webException(Status.NOT_FOUND.getStatusCode(), message, args);
  }

  public static WebApplicationException webException(int statusCode, String message, Object... formatArgs) {
    if (formatArgs != null && formatArgs.length > 0) {
      message = String.format(message, formatArgs);
    }

    throw new WebApplicationException(Response.status(statusCode).entity(message).type("text/plain").build());
  }


}
