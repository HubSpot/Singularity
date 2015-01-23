package com.hubspot.singularity.sentry;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

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

  private String getCallingClassName(StackTraceElement[] stackTrace) {
    if (stackTrace != null && stackTrace.length > 1) {
      return stackTrace[1].getClassName();
    } else {
      return "(unknown)";
    }
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

  private void sendEvent(Raven raven, final EventBuilder eventBuilder) {
    raven.runBuilderHelpers(eventBuilder);

    raven.sendEvent(eventBuilder.build());
  }

  public void notify(Throwable t, Map<String, String> extraData) {
    if (!raven.isPresent()) {
      return;
    }

    final String culprit = determineCulprit(t);

    final EventBuilder eventBuilder = new EventBuilder()
            .setCulprit(getPrefix() + culprit)
            .setMessage(Strings.nullToEmpty(t.getMessage()))
            .setLevel(Event.Level.ERROR)
            .setLogger(getCallingClassName(Thread.currentThread().getStackTrace()))
            .addSentryInterface(new ExceptionInterface(t));

    if (extraData != null && !extraData.isEmpty()) {
      for (Map.Entry<String, String> entry : extraData.entrySet()) {
        eventBuilder.addExtra(entry.getKey(), entry.getValue());
      }
    }

    sendEvent(raven.get(), eventBuilder);
  }

  public void notify(String subject, Map<String, String> extraData) {
    if (!raven.isPresent()) {
      return;
    }

    final EventBuilder eventBuilder = new EventBuilder()
            .setMessage(getPrefix() + subject)
            .setLevel(Event.Level.ERROR)
            .setLogger(getCallingClassName(Thread.currentThread().getStackTrace()));

    if (extraData != null && !extraData.isEmpty()) {
      for (Map.Entry<String, String> entry : extraData.entrySet()) {
        eventBuilder.addExtra(entry.getKey(), entry.getValue());
      }
    }

    sendEvent(raven.get(), eventBuilder);
  }
}
