package com.hubspot.singularity.sentry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;

import net.kencochrane.raven.Raven;
import net.kencochrane.raven.event.Event;
import net.kencochrane.raven.event.EventBuilder;
import net.kencochrane.raven.event.interfaces.ExceptionInterface;

public class SingularityExceptionNotifier {
  private final static Logger LOG = LoggerFactory.getLogger(SingularityExceptionNotifier.class);

  private final Optional<Raven> raven;

  @Inject
  public SingularityExceptionNotifier(Optional<Raven> raven) {
    this.raven = raven;
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
      .setMessage(message)
      .setLevel(Event.Level.ERROR);
    
    sendEvent(raven, eventBuilder);    
  }
  
  private void notify(Raven raven, Throwable t) {
    final EventBuilder eventBuilder = new EventBuilder()
      .setMessage(t.getMessage())
      .setLevel(Event.Level.ERROR)
      .addSentryInterface(new ExceptionInterface(t));

    sendEvent(raven, eventBuilder);
  }
  
  private void sendEvent(Raven raven, final EventBuilder eventBuilder) {
    raven.runBuilderHelpers(eventBuilder);

    raven.sendEvent(eventBuilder.build());    
  }
  
}
