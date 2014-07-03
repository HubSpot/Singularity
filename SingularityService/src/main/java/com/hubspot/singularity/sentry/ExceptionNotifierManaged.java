package com.hubspot.singularity.sentry;

import io.dropwizard.lifecycle.Managed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class ExceptionNotifierManaged implements Managed {
  private final static Logger LOG = LoggerFactory.getLogger(NotifyingExceptionMapper.class);

  private final ExceptionNotifier exceptionNotifier;

  @Inject
  public ExceptionNotifierManaged(ExceptionNotifier exceptionNotifier) {
    this.exceptionNotifier = exceptionNotifier;
  }

  @Override
  public void start() throws Exception {
    LOG.info("Setting NotifyingUncaughtExceptionManager as the default uncaught exception provider...");
    Thread.setDefaultUncaughtExceptionHandler(new NotifyingUncaughtExceptionManager(exceptionNotifier));
  }

  @Override
  public void stop() throws Exception {

  }
}
