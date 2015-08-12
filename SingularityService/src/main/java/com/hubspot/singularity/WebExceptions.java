package com.hubspot.singularity;

import static java.lang.String.format;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.sun.jersey.api.ConflictException;
import com.sun.jersey.api.NotFoundException;

public final class WebExceptions {

  private WebExceptions() {
  }

  public static void checkBadRequest(boolean condition, String message, Object... args) {
    if (!condition) {
      badRequest(message, args);
    }
  }

  public static void checkConflict(boolean condition, String message, Object... args) {
    if (!condition) {
      conflict(message, args);
    }
  }

  public static void checkNotFound(boolean condition, String message, Object... args) {
    if (!condition) {
      notFound(message, args);
    }
  }

  public static void checkForbidden(boolean condition, String message, Object... args) {
    if (!condition) {
      forbidden(message, args);
    }
  }

  public static void checkUnauthorized(boolean condition, String message, Object... args) {
    if (!condition) {
      unauthorized(message, args);
    }
  }

  public static <T> T checkNotNullBadRequest(T value, String message, Object... args) {
    if (value == null) {
      badRequest(message, args);
    }
    return value;
  }

  public static WebApplicationException badRequest(String message, Object... args) {
    throw webException(Status.BAD_REQUEST.getStatusCode(), message, args);
  }

  public static WebApplicationException timeout(String message, Object... args) {
    throw webException(408, message, args);
  }

  public static WebApplicationException conflict(String message, Object... args) {
    if (args.length > 0) {
      message = format(message, args);
    }
    throw new ConflictException(message);
  }

  public static WebApplicationException notFound(String message, Object... args) {
    if (args.length > 0) {
      message = format(message, args);
    }
    throw new NotFoundException(message);
  }

  public static WebApplicationException forbidden(String message, Object... args) {
    return webException(Status.FORBIDDEN.getStatusCode(), message, args);
  }

  public static WebApplicationException unauthorized(String message, Object... args) {
    return webException(Status.UNAUTHORIZED.getStatusCode(), message, args);
  }

  private static WebApplicationException webException(int statusCode, String message, Object... formatArgs) {
    if (formatArgs.length > 0) {
      message = format(message, formatArgs);
    }

    throw new WebApplicationException(Response.status(statusCode).entity(message).type("text/plain").build());
  }

}
