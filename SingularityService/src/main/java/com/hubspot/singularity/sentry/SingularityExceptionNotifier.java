package com.hubspot.singularity.sentry;

import javax.inject.Singleton;

import net.kencochrane.raven.Raven;
import net.kencochrane.raven.RavenFactory;
import net.kencochrane.raven.event.Event;
import net.kencochrane.raven.event.EventBuilder;
import net.kencochrane.raven.event.interfaces.ExceptionInterface;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.hubspot.singularity.config.SentryConfiguration;

@Singleton
public class SingularityExceptionNotifier {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityExceptionNotifier.class);

  private final Optional<Raven> raven;
  private final Optional<SentryConfiguration> sentryConfiguration;

  private final Predicate<StackTraceElement> isSingularityTraceSignature = new Predicate<StackTraceElement>() {
    @Override
    public boolean apply(StackTraceElement input) {
      for (String sig : sentryConfiguration.get().getSingularityTraceSignatures()) {
        if (!input.getClassName().contains(sig)) {
          return true;
        }
      }

      return false;
    }
  };

  private final Predicate<StackTraceElement> isIgnoredStackTraceElement = new Predicate<StackTraceElement>() {
    @Override
    public boolean apply(StackTraceElement input) {
      for (String sig : sentryConfiguration.get().getIgnoredTraceSignatures()) {
        if (input.getClassName().contains(sig)) {
          return true;
        }
      }
      return false;
    }
  };

  @Inject
  public SingularityExceptionNotifier(Optional<SentryConfiguration> sentryConfiguration) {
    this.sentryConfiguration = sentryConfiguration;
    if (sentryConfiguration.isPresent()) {
      this.raven = Optional.of(RavenFactory.ravenInstance(sentryConfiguration.get().getDsn()));
    } else {
      this.raven = Optional.absent();
    }
  }

  private String getPrefix() {
    if (!sentryConfiguration.isPresent() || Strings.isNullOrEmpty(sentryConfiguration.get().getPrefix())) {
      return "";
    }

    return sentryConfiguration.get().getPrefix() + " ";
  }

  public void notify(Throwable t, Class<?> logger) {
    if (!raven.isPresent()) {
      return;
    }

    try {
      notify(raven.get(), t, logger);
    } catch (Throwable e) {
      LOG.error("Caught exception while trying to report {} to Sentry", t.getMessage(), e);
    }
  }

  public void notify(String message, Class<?> logger) {
    if (!raven.isPresent()) {
      return;
    }

    try {
      notify(raven.get(), message, logger);
    } catch (Throwable e) {
      LOG.error("Caught exception while trying to report {} to Sentry", message, e);
    }
  }

  private void notify(Raven raven, String message, Class<?> logger) {
    final EventBuilder eventBuilder = new EventBuilder()
      .setMessage(getPrefix() + message)
      .setLogger(logger.getName())
      .setLevel(Event.Level.ERROR);

    sendEvent(raven, eventBuilder);
  }

  private void notify(Raven raven, Throwable t, Class<?> logger) {
    final String culprit = determineCulprit(t);

    final EventBuilder eventBuilder = new EventBuilder()
      .setCulprit(getPrefix() + culprit)
      .setMessage(Strings.nullToEmpty(t.getMessage()))
      .setLevel(Event.Level.ERROR)
      .setLogger(logger.getName())
      .addSentryInterface(new ExceptionInterface(t));

    sendEvent(raven, eventBuilder);
  }

  private void sendEvent(Raven raven, final EventBuilder eventBuilder) {
    raven.runBuilderHelpers(eventBuilder);

    raven.sendEvent(eventBuilder.build());
  }

  private String determineCulprit(final Throwable exception) {
    Throwable cause = exception;
    String culprit = null;

    while (cause != null) {
      final List<StackTraceElement> elements = new ArrayList<>(cause.getStackTrace().length);

      if(elements.size() > 0){
        final StackTraceElement root = Iterables.tryFind(elements, Predicates.not(isIgnoredStackTraceElement)).or(elements.get(0));
        final Optional<StackTraceElement> lastSingularityElement = Iterables.tryFind(elements, Predicates.and(isSingularityTraceSignature, Predicates.not(isIgnoredStackTraceElement)));

        final String singularityCulprit = lastSingularityElement.isPresent() ? String.format("%s.%s():%s", lastSingularityElement.get().getClassName(), lastSingularityElement.get().getMethodName(), lastSingularityElement.get().getLineNumber()) : "";
        culprit = String.format("%s.%s():%s:%s|%s", root.getClassName(), root.getMethodName(), root.getLineNumber(), Throwables.getRootCause(exception).getClass().getName(), singularityCulprit);
      }

      cause = cause.getCause();
    }

    return culprit;
  }

}
