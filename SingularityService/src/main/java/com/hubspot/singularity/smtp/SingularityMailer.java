package com.hubspot.singularity.smtp;

import java.util.List;
import java.util.Properties;

import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.singularity.config.SMTPConfiguration;

public class SingularityMailer {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityMailer.class);

  private final Optional<SMTPConfiguration> maybeSmtpConfiguration;

  @Inject
  public SingularityMailer(Optional<SMTPConfiguration> maybeSmtpConfiguration) {
    this.maybeSmtpConfiguration = maybeSmtpConfiguration;
  }

  private String getEmailLogFormat(List<String> toList, String subject, String body) {
    return String.format("[to: %s, subject: %s, body: %s]", toList, subject, body);
  }
  
  public void sendMail(List<String> toList, String subject, String body) {
    if (!maybeSmtpConfiguration.isPresent()) {
      LOG.warn(String.format("Couldn't send email %s because no SMTP configuration is present", getEmailLogFormat(toList, subject, body)));
    }
    
    SMTPConfiguration smtpConfiguration = maybeSmtpConfiguration.get();
    
    boolean useAuth = false;
    
    if (smtpConfiguration.getUsername().isPresent() && smtpConfiguration.getPassword().isPresent()) {
      useAuth = true;
    } else if (smtpConfiguration.getUsername().isPresent() || smtpConfiguration.getPassword().isPresent()) {
      LOG.warn(String.format("Not using smtp authentication because username (%s) or password (%s) was not present", smtpConfiguration.getUsername().isPresent(), smtpConfiguration.getPassword().isPresent()));
    }
    
    try {
      Properties properties = System.getProperties();

      properties.setProperty("mail.smtp.host", smtpConfiguration.getHost());
      
      if (smtpConfiguration.getPort().isPresent()) {
        properties.setProperty("mail.smtp.port", Integer.toString(smtpConfiguration.getPort().get()));
      }
      
      if (useAuth) {
        properties.setProperty("mail.smtp.auth", "true");
      }
      
      Session session = Session.getDefaultInstance(properties);
      Transport transport = session.getTransport("smtp");
      
      if (useAuth) {
        transport.connect(smtpConfiguration.getUsername().get(), smtpConfiguration.getPassword().get());
      }
      
      MimeMessage message = new MimeMessage(session);

      message.setFrom(new InternetAddress(smtpConfiguration.getFrom()));
      
      List<InternetAddress> addresses = Lists.newArrayList();
      
      for (String to : toList) {
        addresses.add(new InternetAddress(to));
      }

      message.setSubject(subject);
      message.setText(body);
      
      transport.sendMessage(message, addresses.toArray(new InternetAddress[addresses.size()]));
    } catch (Throwable t) {
      LOG.warn(String.format("Unable to send message [to: %s, subject: %s, body: %s] due to exception", toList, subject, body), t);
    }
  }

}
