package com.hubspot.singularity.sentry;

import io.dropwizard.jersey.errors.LoggingExceptionMapper;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;

import net.kencochrane.raven.Raven;

@javax.ws.rs.ext.Provider
public class SentryExceptionMapper extends LoggingExceptionMapper<Exception> {
  private final static Logger LOG = LoggerFactory.getLogger(SentryExceptionMapper.class);

  private final Optional<Raven> raven;

  @Inject
  public SentryExceptionMapper(Optional<Raven> raven) {
    this.raven = raven;
  }

  @Override
  public Response toResponse(final Exception e) {
    final Response response = super.toResponse(e);

    if (response.getStatus() >= 500 && raven.isPresent()) {
      LOG.info("Sending exception to Sentry...");
      raven.get().sendException(e);
    }

    return response;
  }
}

