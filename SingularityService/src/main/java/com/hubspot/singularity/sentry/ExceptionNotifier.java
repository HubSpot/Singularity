package com.hubspot.singularity.sentry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;

import net.kencochrane.raven.Raven;
import net.kencochrane.raven.event.Event;
import net.kencochrane.raven.event.EventBuilder;
import net.kencochrane.raven.event.interfaces.ExceptionInterface;

public class ExceptionNotifier {
  private final static Logger LOG = LoggerFactory.getLogger(ExceptionNotifier.class);

  private final Optional<Raven> raven;

  @Inject
  public ExceptionNotifier(Optional<Raven> raven) {
    this.raven = raven;
  }

  public void notify(Throwable t) {
    try {
      if (raven.isPresent()) {
        final EventBuilder eventBuilder = new EventBuilder()
            .setMessage(t.getMessage())
            .setLevel(Event.Level.ERROR)
            .addSentryInterface(new ExceptionInterface(t));

        raven.get().runBuilderHelpers(eventBuilder);

        raven.get().sendEvent(eventBuilder.build());
      }
    } catch (Exception e) {
      LOG.error("Caught exception while trying to report to Sentry", e);
    }
  }
}
