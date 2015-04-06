package com.hubspot.singularity.sentry;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotifyingUncaughtExceptionManager implements UncaughtExceptionHandler {
  private static final Logger LOG = LoggerFactory.getLogger(NotifyingUncaughtExceptionManager.class);

  private final SingularityExceptionNotifier notifier;

  public NotifyingUncaughtExceptionManager(SingularityExceptionNotifier notifier) {
    this.notifier = notifier;
  }

  @Override
  public void uncaughtException(Thread t, Throwable e) {
    LOG.error("Uncaught exception!", e);
    notifier.notify(e, Collections.<String, String>emptyMap());
  }
}
