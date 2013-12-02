package com.hubspot.singularity.smtp;

import java.util.List;
import java.util.Properties;

import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.config.SMTPConfiguration;

public class SingularityMailer {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityMailer.class);

  private final SMTPConfiguration smtpConfiguration;

  @Inject
  public SingularityMailer(SMTPConfiguration smtpConfiguration) {
    this.smtpConfiguration = smtpConfiguration;
  }
  
  public void sendAbortNotification() {
    sendMail(smtpConfiguration.getAdmins(), String.format("Singularity on %s is aborting!", JavaUtils.getHostName()), "If are you using monit to restart Singularity, this is just a notification (Singularity should restart.)");
  }
  
  public void sendMail(List<String> toList, String subject, String body) {
    try {
      Preconditions.checkArgument(smtpConfiguration.getUsername() != null, "Username must be specified to use SMTP");
      Preconditions.checkArgument(smtpConfiguration.getHost() != null, "Host must be specified to use SMTP");
      Preconditions.checkArgument(smtpConfiguration.getPassword() != null, "Password must be specified to use SMTP");
      
      Properties properties = System.getProperties();

      properties.setProperty("mail.smtp.host", smtpConfiguration.getHost());
      
      if (smtpConfiguration.getPort().isPresent()) {
        properties.setProperty("mail.smtp.port", Integer.toString(smtpConfiguration.getPort().get()));
      }
      
      properties.setProperty("mail.smtp.auth", "true");
      
      Session session = Session.getDefaultInstance(properties);
      Transport transport = session.getTransport("smtp");
      transport.connect(smtpConfiguration.getUsername(), smtpConfiguration.getPassword());
      
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
