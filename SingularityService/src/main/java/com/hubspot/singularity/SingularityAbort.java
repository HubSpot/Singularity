package com.hubspot.singularity;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Singleton;

import io.dropwizard.lifecycle.Managed;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.config.SMTPConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;
import com.hubspot.singularity.smtp.SingularitySmtpSender;

import ch.qos.logback.classic.LoggerContext;

@Singleton
public class SingularityAbort implements Managed, ConnectionStateListener {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityAbort.class);

  private final Optional<SMTPConfiguration> maybeSmtpConfiguration;
  private final SingularitySmtpSender smtpSender;
  private final SingularityExceptionNotifier exceptionNotifier;

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
  public void start() {
  }

  @Override
  public void stop() {
    flushLogs();
  }

  @Override
  public void stateChanged(CuratorFramework client, ConnectionState newState) {
    if (newState == ConnectionState.LOST) {
      LOG.error("Aborting due to new connection state received from ZooKeeper: {}", newState);
      abort();
    }
  }

  public void abort() {
    if (!aborting.getAndSet(true)) {
      sendAbortMail();
      flushLogs();
      exit();
    }
  }

  private void exit() {
    // TODO - this one is terrifying. Even though it is fine that singularity wants to terminate itself, it should do so through the dw framework to
    // ensure that the managed objects are shut down correctly.
    System.exit(1);
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
