package com.hubspot.singularity.sentry;

import java.util.Map;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.hubspot.singularity.SingularityMainModule;
import com.hubspot.singularity.config.SentryConfiguration;

import net.kencochrane.raven.Raven;
import net.kencochrane.raven.RavenFactory;
import net.kencochrane.raven.event.Event;
import net.kencochrane.raven.event.EventBuilder;
import net.kencochrane.raven.event.interfaces.ExceptionInterface;

@Singleton
public class SingularityExceptionNotifier {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityExceptionNotifier.class);

  private final Optional<Raven> raven;
  private final Optional<SentryConfiguration> sentryConfiguration;
  private final Provider<Optional<HttpServletRequest>> requestProvider;

  @Inject
  public SingularityExceptionNotifier(Optional<SentryConfiguration> sentryConfiguration, @Named(SingularityMainModule.CURRENT_HTTP_REQUEST) Provider<Optional<HttpServletRequest>> requestProvider) {
    this.sentryConfiguration = sentryConfiguration;
    this.requestProvider = requestProvider;
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

  private Optional<String> getCurrentUrl() {
    final Optional<HttpServletRequest> request = requestProvider.get();

    if (request.isPresent()) {
      return Optional.of(request.get().getRequestURI());
    } else {
      return Optional.absent();
    }
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

  public void notify(Throwable t, Map<String, String> extraData) {
    if (!raven.isPresent()) {
      return;
    }

    final StackTraceElement[] currentThreadStackTrace = Thread.currentThread().getStackTrace();

    final EventBuilder eventBuilder = new EventBuilder()
            .withCulprit(getPrefix() + t.getMessage())
            .withMessage(Strings.nullToEmpty(t.getMessage()))
            .withLevel(Event.Level.ERROR)
            .withLogger(getCallingClassName(currentThreadStackTrace))
            .withSentryInterface(new ExceptionInterface(t))
            .withExtra("url", getCurrentUrl().or("none"));

    if (extraData != null && !extraData.isEmpty()) {
      for (Map.Entry<String, String> entry : extraData.entrySet()) {
        eventBuilder.withExtra(entry.getKey(), entry.getValue());
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
            .withLogger(getCallingClassName(currentThreadStackTrace))
            .withExtra("url", getCurrentUrl().or("none"));

    if (extraData != null && !extraData.isEmpty()) {
      for (Map.Entry<String, String> entry : extraData.entrySet()) {
        eventBuilder.withExtra(entry.getKey(), entry.getValue());
      }
    }

    sendEvent(raven.get(), eventBuilder);
  }
}
