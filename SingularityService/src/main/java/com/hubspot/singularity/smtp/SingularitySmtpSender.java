package com.hubspot.singularity.smtp;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;
import javax.mail.Address;
import javax.mail.Message.RecipientType;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.config.SMTPConfiguration;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

import io.dropwizard.lifecycle.Managed;

@Singleton
public class SingularitySmtpSender implements Managed {

  private static final Logger LOG = LoggerFactory.getLogger(SingularitySmtpSender.class);

  private final Optional<SMTPConfiguration> maybeSmtpConfiguration;
  private final Optional<ThreadPoolExecutor> mailSenderExecutorService;
  private final SingularityExceptionNotifier exceptionNotifier;

  @Inject
  public SingularitySmtpSender(Optional<SMTPConfiguration> maybeSmtpConfiguration, SingularityExceptionNotifier exceptionNotifier) {
    this.maybeSmtpConfiguration = maybeSmtpConfiguration;
    this.exceptionNotifier = exceptionNotifier;

    if (maybeSmtpConfiguration.isPresent()) {
      this.mailSenderExecutorService = Optional.of(JavaUtils.newFixedTimingOutThreadPool(maybeSmtpConfiguration.get().getMailMaxThreads(), TimeUnit.SECONDS.toMillis(1), "SingularitySMTPSender-%d"));
    } else {
      this.mailSenderExecutorService = Optional.absent();
    }
  }

  @Override
  public void start() {}

  @Override
  public void stop() {
    if (mailSenderExecutorService.isPresent()) {
      MoreExecutors.shutdownAndAwaitTermination(mailSenderExecutorService.get(), 1, TimeUnit.SECONDS);
    }
  }

  public void queueMail(final List<String> toList, final List<String> ccList, final String subject, final String body) {
    if (toList.isEmpty()) {
      LOG.warn("Couldn't queue email {} because no to address is present", subject);
      return;
    }

    if (!mailSenderExecutorService.isPresent()) {
      LOG.warn(String.format("Couldn't queue email %s because no SMTP configuration is present", getEmailLogFormat(toList, subject)));
      return;
    }

    final String realSubject = maybeSmtpConfiguration.isPresent() && maybeSmtpConfiguration.get().getSubjectPrefix().isPresent() ?
        maybeSmtpConfiguration.get().getSubjectPrefix().get() + subject :
        subject;

    LOG.debug("Queuing an email to {}/{} (subject: {})", toList, ccList, realSubject);

    mailSenderExecutorService.get().submit(() -> sendMail(toList, ccList, realSubject, body));
  }

  private String getEmailLogFormat(List<String> toList, String subject) {
    return String.format("[to: %s, subject: %s]", toList, subject);
  }

  private Session createSession(SMTPConfiguration smtpConfiguration, boolean useAuth) {
    Properties properties = System.getProperties();

    properties.setProperty("mail.smtp.host", smtpConfiguration.getHost());
    properties.setProperty("mail.smtp.port", Integer.toString(smtpConfiguration.getPort()));

    if (smtpConfiguration.isSsl()) {
      properties.setProperty("mail.smtp.ssl.enable", "true");
    }

    if (useAuth) {
      properties.setProperty("mail.smtp.auth", "true");
      return Session.getInstance(properties, new SMTPAuthenticator(smtpConfiguration.getUsername().get(), smtpConfiguration.getPassword().get()));
    } else {
      return Session.getInstance(properties);
    }
  }

  private void sendMail(List<String> toList, List<String> ccList, String subject, String body) {
    final SMTPConfiguration smtpConfiguration = maybeSmtpConfiguration.get();

    boolean useAuth = false;

    if (smtpConfiguration.getUsername().isPresent() && smtpConfiguration.getPassword().isPresent()) {
      useAuth = true;
    } else if (smtpConfiguration.getUsername().isPresent() || smtpConfiguration.getPassword().isPresent()) {
      LOG.warn("Not using smtp authentication because username ({}) or password ({}) was not present", smtpConfiguration.getUsername().isPresent(), smtpConfiguration.getPassword().isPresent());
    }

    try {
      final Session session = createSession(smtpConfiguration, useAuth);

      MimeMessage message = new MimeMessage(session);

      Address[] toArray = getAddresses(toList);
      message.addRecipients(RecipientType.TO, toArray);

      if (!ccList.isEmpty()) {
        Address[] ccArray = getAddresses(ccList);
        message.addRecipients(RecipientType.CC, ccArray);
      }

      message.setFrom(new InternetAddress(smtpConfiguration.getFrom()));

      message.setSubject(MimeUtility.encodeText(subject, "utf-8", null));
      message.setContent(body, "text/html; charset=utf-8");

      LOG.trace("Sending a message to {} - {}", Arrays.toString(toArray), getEmailLogFormat(toList, subject));

      Transport.send(message);
    } catch (Throwable t) {
      LOG.warn("Unable to send message {}", getEmailLogFormat(toList, subject), t);
      exceptionNotifier.notify(String.format("Unable to send message (%s)", t.getMessage()), t, ImmutableMap.of("subject", subject));
    }
  }

  private Address[] getAddresses(List<String> toList) {
    List<InternetAddress> addresses = Lists.newArrayListWithCapacity(toList.size());

    for (String to : toList) {
      try {
        addresses.add(new InternetAddress(to));
      } catch (AddressException t) {
        LOG.warn("Invalid address {} - ignoring", to, t);
      }
    }

    return addresses.toArray(new InternetAddress[addresses.size()]);
  }
}
