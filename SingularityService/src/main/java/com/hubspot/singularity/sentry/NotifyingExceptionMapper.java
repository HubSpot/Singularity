package com.hubspot.singularity.sentry;

import javax.ws.rs.core.Response;

import com.google.inject.Inject;

import io.dropwizard.jersey.errors.LoggingExceptionMapper;

@javax.ws.rs.ext.Provider
public class NotifyingExceptionMapper extends LoggingExceptionMapper<Exception> {
  private final SingularityExceptionNotifier notifier;

  @Inject
  public NotifyingExceptionMapper(SingularityExceptionNotifier notifier) {
    this.notifier = notifier;
  }

  @Override
  public Response toResponse(final Exception e) {
    final Response response = super.toResponse(e);

    if (response.getStatus() >= 500) {
      notifier.notify(String.format("Uncaught Exception (%s)", e.getMessage()), e);
    }

    return response;
  }
}

