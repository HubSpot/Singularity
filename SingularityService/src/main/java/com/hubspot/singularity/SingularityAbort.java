package com.hubspot.singularity;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Singleton;

import io.dropwizard.lifecycle.Managed;
import io.dropwizard.lifecycle.ServerLifecycleListener;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.eclipse.jetty.server.Server;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.config.SMTPConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;
import com.hubspot.singularity.smtp.SingularitySmtpSender;

@Singleton
public class SingularityAbort implements Managed, ConnectionStateListener, ServerLifecycleListener {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityAbort.class);

  private final Optional<SMTPConfiguration> maybeSmtpConfiguration;
  private final SingularitySmtpSender smtpSender;
  private final SingularityExceptionNotifier exceptionNotifier;

  private final AtomicReference<Server> serverHolder = new AtomicReference<>();
  private final AtomicBoolean aborting = new AtomicBoolean();

  @Inject
  public SingularityAbort(SingularitySmtpSender smtpSender,
      SingularityConfiguration configuration,
      SingularityExceptionNotifier exceptionNotifier) {
    this.maybeSmtpConfiguration = configuration.getSmtpConfiguration();
    this.smtpSender = smtpSender;
    this.exceptionNotifier = exceptionNotifier;
  }

  @Override
  public void start() {}

  @Override
  public void stop() {
    // Allow GC'ing of the server object.
    serverHolder.set(null);
    flushLogs();
  }

  @Override
  public void stateChanged(CuratorFramework client, ConnectionState newState) {
    if (newState == ConnectionState.LOST) {
      LOG.error("Aborting due to new connection state received from ZooKeeper: {}", newState);
      abort();
    }
  }

  @Override
  public void serverStarted(Server server) {
    serverHolder.set(server);
  }

  public void abort() {
    if (!aborting.getAndSet(true)) {
      sendAbortMail();
      flushLogs();
      exit();
    }
  }

  private void exit() {
    Server server = serverHolder.getAndSet(null);
    if (server != null) {
      try {
        server.stop();
      } catch (Exception e) {
        LOG.warn("While aborting server", e);
      }
    } else {
      LOG.warn("SingularityAbort called before server has fully initialized!");
      System.exit(1); // Use the hammer.
    }
  }

  private void sendAbortMail() {
    if (!maybeSmtpConfiguration.isPresent()) {
      LOG.warn("Couldn't send abort mail because no SMTP configuration is present");
      return;
    }

    final List<String> to = maybeSmtpConfiguration.get().getAdmins();
    final String subject = String.format("Singularity on %s is aborting", JavaUtils.getHostName());

    smtpSender.queueMail(to, ImmutableList.<String>of(), subject, "");
  }

  private void flushLogs() {
    LOG.info("Attempting to flush logs and wait for 5 seconds...");

    ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
    if (loggerFactory instanceof LoggerContext) {
      LoggerContext context = (LoggerContext) loggerFactory;
      context.stop();
    }

    try {
      Thread.sleep(5000);
    } catch (Exception e) {
      LOG.info("While sleeping for log flush", e);
      exceptionNotifier.notify(e);
    }
  }
}
