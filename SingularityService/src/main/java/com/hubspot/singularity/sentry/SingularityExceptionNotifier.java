package com.hubspot.singularity.sentry;

import javax.inject.Singleton;

import net.kencochrane.raven.Raven;
import net.kencochrane.raven.RavenFactory;
import net.kencochrane.raven.event.Event;
import net.kencochrane.raven.event.EventBuilder;
import net.kencochrane.raven.event.interfaces.ExceptionInterface;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.hubspot.singularity.config.SentryConfiguration;

@Singleton
public class SingularityExceptionNotifier {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityExceptionNotifier.class);

  private final Optional<Raven> raven;
  private final Optional<SentryConfiguration> sentryConfiguration;

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

  public void notify(Throwable t) {
    if (!raven.isPresent()) {
      return;
    }

    try {
      notify(raven.get(), t);
    } catch (Throwable e) {
      LOG.error("Caught exception while trying to report {} to Sentry", t.getMessage(), e);
    }
  }

  public void notify(String message) {
    if (!raven.isPresent()) {
      return;
    }

    try {
      notify(raven.get(), message);
    } catch (Throwable e) {
      LOG.error("Caught exception while trying to report {} to Sentry", message, e);
    }
  }

  private void notify(Raven raven, String message) {
    final EventBuilder eventBuilder = new EventBuilder()
      .setMessage(getPrefix() + message)
      .setLevel(Event.Level.ERROR);

    sendEvent(raven, eventBuilder);
  }

  private void notify(Raven raven, Throwable t) {
    final EventBuilder eventBuilder = new EventBuilder()
      .setMessage(getPrefix() + t.getMessage())
      .setLevel(Event.Level.ERROR)
      .addSentryInterface(new ExceptionInterface(t));

    sendEvent(raven, eventBuilder);
  }

  private void sendEvent(Raven raven, final EventBuilder eventBuilder) {
    raven.runBuilderHelpers(eventBuilder);

    raven.sendEvent(eventBuilder.build());
  }

}
