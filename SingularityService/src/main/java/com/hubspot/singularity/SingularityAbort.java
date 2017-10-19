package com.hubspot.singularity;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.eclipse.jetty.server.Server;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HostAndPort;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.config.SMTPConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;
import com.hubspot.singularity.smtp.SingularitySmtpSender;

import ch.qos.logback.classic.LoggerContext;

@Singleton
public class SingularityAbort implements ConnectionStateListener {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityAbort.class);

  private final Optional<SMTPConfiguration> maybeSmtpConfiguration;
  private final SingularitySmtpSender smtpSender;
  private final HostAndPort hostAndPort;
  private final SingularityExceptionNotifier exceptionNotifier;
  private final Injector injector;

  private final ServerProvider serverProvider;
  private final AtomicBoolean aborting = new AtomicBoolean();

  @Inject
  public SingularityAbort(SingularitySmtpSender smtpSender,
                          ServerProvider serverProvider,
                          SingularityConfiguration configuration,
                          SingularityExceptionNotifier exceptionNotifier,
                          Injector injector,
                          @Named(SingularityMainModule.HTTP_HOST_AND_PORT) HostAndPort hostAndPort) {
    this.maybeSmtpConfiguration = configuration.getSmtpConfigurationOptional();
    this.serverProvider = serverProvider;
    this.smtpSender = smtpSender;
    this.exceptionNotifier = exceptionNotifier;
    this.injector = injector;
    this.hostAndPort = hostAndPort;
  }

  @Override
  public void stateChanged(CuratorFramework client, ConnectionState newState) {
    if (newState == ConnectionState.LOST) {
      LOG.error("Aborting due to new connection state received from ZooKeeper: {}", newState);
      abort(AbortReason.LOST_ZK_CONNECTION, Optional.<Throwable>absent());
    }
  }

  public enum AbortReason {
    LOST_ZK_CONNECTION, LOST_LEADERSHIP, UNRECOVERABLE_ERROR, TEST_ABORT, MESOS_ERROR, LOST_MESOS_CONNECTION;
  }

  public void abort(AbortReason abortReason, Optional<Throwable> throwable) {
    if (!aborting.getAndSet(true)) {
      try {
        sendAbortNotification(abortReason, throwable);
        if (abortReason != AbortReason.LOST_LEADERSHIP && abortReason != AbortReason.LOST_ZK_CONNECTION) {
          attemptLeaderLatchClose();
        }

        flushLogs();
      } finally {
        exit();
      }
    }
  }

  private void attemptLeaderLatchClose() {
    try {
      injector.getInstance(LeaderLatch.class).close();
    } catch (Exception e) {
      LOG.error("While attempting to close leader latch", e);
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

  private void sendAbortNotification(AbortReason abortReason, Optional<Throwable> throwable) {
    final String message = String.format("Singularity on %s is aborting due to %s", hostAndPort.getHostText(), abortReason);

    LOG.error(message);

    sendAbortMail(message, throwable);

    exceptionNotifier.notify(message, ImmutableMap.of("abortReason", abortReason.name()));
  }

  private void sendAbortMail(final String message, final Optional<Throwable> throwable) {
    if (!maybeSmtpConfiguration.isPresent()) {
      LOG.warn("Couldn't send abort mail because no SMTP configuration is present");
      return;
    }

    final List<SingularityEmailDestination> emailDestination = maybeSmtpConfiguration.get().getEmailConfiguration().get(SingularityEmailType.SINGULARITY_ABORTING);

    if (emailDestination.isEmpty() || !emailDestination.contains(SingularityEmailDestination.ADMINS)) {
      LOG.info("Not configured to send abort mail");
      return;
    }

    final String body = throwable.isPresent() ? throwable.get().toString() : "(no stack trace)";

    smtpSender.queueMail(maybeSmtpConfiguration.get().getAdmins(), ImmutableList.<String> of(), message, body);
  }

  private void flushLogs() {
    final long millisToWait = 100;

    LOG.info("Attempting to flush logs and wait {} ...", JavaUtils.durationFromMillis(millisToWait));

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
