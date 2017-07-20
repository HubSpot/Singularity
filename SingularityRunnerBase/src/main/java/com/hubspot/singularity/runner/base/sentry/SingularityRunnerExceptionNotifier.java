package com.hubspot.singularity.runner.base.sentry;

import java.util.Map;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.runner.base.configuration.SingularityRunnerBaseConfiguration;

import net.kencochrane.raven.Raven;
import net.kencochrane.raven.RavenFactory;
import net.kencochrane.raven.event.Event;
import net.kencochrane.raven.event.EventBuilder;
import net.kencochrane.raven.event.interfaces.ExceptionInterface;

@Singleton
public class SingularityRunnerExceptionNotifier {
  private final Optional<Raven> raven;
  private final SingularityRunnerBaseConfiguration configuration;

  @Inject
  public SingularityRunnerExceptionNotifier(SingularityRunnerBaseConfiguration configuration) {
    this.configuration = configuration;
    if (configuration.getSentryDsn().isPresent()) {
      this.raven = Optional.of(RavenFactory.ravenInstance(configuration.getSentryDsn().get()));
    } else {
      this.raven = Optional.absent();
    }
  }

  private String getPrefix() {
    if (Strings.isNullOrEmpty(configuration.getSentryPrefix())) {
      return "";
    }

    return configuration.getSentryPrefix() + " ";
  }

  private String getCallingClassName(StackTraceElement[] stackTrace) {
    if (stackTrace != null && stackTrace.length > 2) {
      return stackTrace[2].getClassName();
    } else {
      return "(unknown)";
    }
  }

  private void sendEvent(Raven raven, final EventBuilder eventBuilder) {
    raven.runBuilderHelpers(eventBuilder);

    raven.sendEvent(eventBuilder.build());
  }

  public void notify(String message, Throwable t, Map<String, String> extraData) {
    if (!raven.isPresent()) {
      return;
    }

    final StackTraceElement[] currentThreadStackTrace = Thread.currentThread().getStackTrace();

    final EventBuilder eventBuilder = new EventBuilder()
            .withCulprit(getPrefix() + message)
            .withMessage(Strings.nullToEmpty(message))
            .withLevel(Event.Level.ERROR)
            .withLogger(getCallingClassName(currentThreadStackTrace))
            .withSentryInterface(new ExceptionInterface(t));

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

    final StackTraceElement[] currentThreadStackTrace = Thread.currentThread().getStackTrace();

    final EventBuilder eventBuilder = new EventBuilder()
            .withMessage(getPrefix() + subject)
            .withLevel(Event.Level.ERROR)
            .withLogger(getCallingClassName(currentThreadStackTrace));

    if (extraData != null && !extraData.isEmpty()) {
      for (Map.Entry<String, String> entry : extraData.entrySet()) {
        eventBuilder.withExtra(entry.getKey(), entry.getValue());
      }
    }

    sendEvent(raven.get(), eventBuilder);
  }
}
