package com.hubspot.singularity;

import ch.qos.logback.classic.LoggerContext;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HostAndPort;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.config.SMTPConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.managed.SingularityLifecycleManaged;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;
import com.hubspot.singularity.smtp.SingularitySmtpSender;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.jetty.server.Server;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SingularityAbort {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityAbort.class);

  private final Optional<SMTPConfiguration> maybeSmtpConfiguration;
  private final SingularitySmtpSender smtpSender;
  private final HostAndPort hostAndPort;
  private final SingularityExceptionNotifier exceptionNotifier;
  private final Injector injector;

  private final ServerProvider serverProvider;
  private final AtomicBoolean aborting = new AtomicBoolean();

  @Inject
  public SingularityAbort(
    SingularitySmtpSender smtpSender,
    ServerProvider serverProvider,
    SingularityConfiguration configuration,
    SingularityExceptionNotifier exceptionNotifier,
    Injector injector,
    @Named(SingularityMainModule.HTTP_HOST_AND_PORT) HostAndPort hostAndPort
  ) {
    this.maybeSmtpConfiguration = configuration.getSmtpConfigurationOptional();
    this.serverProvider = serverProvider;
    this.smtpSender = smtpSender;
    this.exceptionNotifier = exceptionNotifier;
    this.injector = injector;
    this.hostAndPort = hostAndPort;
  }

  public enum AbortReason {
    LOST_ZK_CONNECTION,
    LOST_LEADERSHIP,
    UNRECOVERABLE_ERROR,
    ERROR_IN_LEADER_ONLY_POLLER,
    TEST_ABORT,
    MESOS_ERROR,
    LOST_MESOS_CONNECTION
  }

  public void abort(AbortReason abortReason, Optional<Throwable> throwable) {
    if (!aborting.getAndSet(true)) {
      try {
        sendAbortNotification(abortReason, throwable);
        SingularityLifecycleManaged lifecycle = injector.getInstance(
          SingularityLifecycleManaged.class
        );
        try {
          lifecycle.stop();
        } catch (Throwable t) {
          LOG.error("While shutting down", t);
        }
        flushLogs();
      } finally {
        exit();
      }
    }
  }

  private void exit() {
    Optional<Server> server = serverProvider.get();
    if (server.isPresent()) {
      try {
        server.get().stop();
      } catch (Exception e) {
        LOG.warn("While aborting server", e);
      } finally {
        System.exit(1);
      }
    } else {
      LOG.warn("SingularityAbort called before server has fully initialized!");
      System.exit(1); // Use the hammer.
    }
  }

  private void sendAbortNotification(
    AbortReason abortReason,
    Optional<Throwable> throwable
  ) {
    final String message = String.format(
      "Singularity on %s is aborting due to %s",
      hostAndPort.getHost(),
      abortReason
    );

    LOG.error(message);

    sendAbortMail(message, throwable);

    if (throwable.isPresent()) {
      exceptionNotifier.notify(
        message,
        throwable.get(),
        ImmutableMap.of("abortReason", abortReason.name())
      );
    } else {
      exceptionNotifier.notify(
        message,
        ImmutableMap.of("abortReason", abortReason.name())
      );
    }
  }

  private void sendAbortMail(final String message, final Optional<Throwable> throwable) {
    if (!maybeSmtpConfiguration.isPresent()) {
      LOG.warn("Couldn't send abort mail because no SMTP configuration is present");
      return;
    }

    final List<SingularityEmailDestination> emailDestination = maybeSmtpConfiguration
      .get()
      .getEmailConfiguration()
      .get(SingularityEmailType.SINGULARITY_ABORTING);

    if (
      emailDestination.isEmpty() ||
      !emailDestination.contains(SingularityEmailDestination.ADMINS)
    ) {
      LOG.info("Not configured to send abort mail");
      return;
    }

    final String body;
    if (throwable.isPresent()) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      throwable.get().printStackTrace(pw);
      body = "<pre>\n" + throwable.get().getMessage() + "\n" + sw.toString() + "\n</pre>";
    } else {
      body = "(no stack trace)";
    }

    smtpSender.queueMail(
      maybeSmtpConfiguration.get().getAdmins(),
      ImmutableList.of(),
      message,
      body
    );
  }

  private void flushLogs() {
    final long millisToWait = 100;

    LOG.info(
      "Attempting to flush logs and wait {} ...",
      JavaUtils.durationFromMillis(millisToWait)
    );

    ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
    if (loggerFactory instanceof LoggerContext) {
      LoggerContext context = (LoggerContext) loggerFactory;
      context.stop();
    }

    try {
      Thread.sleep(millisToWait);
    } catch (Exception e) {
      LOG.info("While sleeping for log flush", e);
    }
  }
}
