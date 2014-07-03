package com.hubspot.singularity.sentry;

import java.lang.Thread.UncaughtExceptionHandler;

public class NotifyingUncaughtExceptionManager implements UncaughtExceptionHandler {
  private final ExceptionNotifier notifier;

  public NotifyingUncaughtExceptionManager(ExceptionNotifier notifier) {
    this.notifier = notifier;
  }

  @Override
  public void uncaughtException(Thread t, Throwable e) {
    notifier.notify(e);
  }
}
