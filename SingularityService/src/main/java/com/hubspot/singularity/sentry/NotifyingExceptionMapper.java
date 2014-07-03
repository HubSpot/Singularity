package com.hubspot.singularity.sentry;

import io.dropwizard.jersey.errors.LoggingExceptionMapper;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

@javax.ws.rs.ext.Provider
public class NotifyingExceptionMapper extends LoggingExceptionMapper<Exception> {
  private final static Logger LOG = LoggerFactory.getLogger(NotifyingExceptionMapper.class);

  private final ExceptionNotifier notifier;

  @Inject
  public NotifyingExceptionMapper(ExceptionNotifier notifier) {
    this.notifier = notifier;
  }

  @Override
  public Response toResponse(final Exception e) {
    final Response response = super.toResponse(e);

    if (response.getStatus() >= 500) {
      notifier.notify(e);
    }

    return response;
  }
}

