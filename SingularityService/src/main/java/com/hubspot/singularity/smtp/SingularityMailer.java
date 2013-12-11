package com.hubspot.singularity.smtp;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.mesos.Protos.TaskState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityCloseable;
import com.hubspot.singularity.SingularityCloser;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.SMTPConfiguration;

public class SingularityMailer implements SingularityCloseable {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityMailer.class);

  private final Optional<SMTPConfiguration> maybeSmtpConfiguration;
  private final Optional<ThreadPoolExecutor> mailSenderExecutorService;
  
  private final SingularityCloser closer;
  
  @Inject
  public SingularityMailer(Optional<SMTPConfiguration> maybeSmtpConfiguration, SingularityCloser closer) {
    this.maybeSmtpConfiguration = maybeSmtpConfiguration;
    this.closer = closer;
    
    if (maybeSmtpConfiguration.isPresent()) {
      this.mailSenderExecutorService = Optional.of(new ThreadPoolExecutor(maybeSmtpConfiguration.get().getMailThreads(), maybeSmtpConfiguration.get().getMailMaxThreads(), 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactoryBuilder().setNameFormat("SingularityMailer-%d").build()));
    } else {
      this.mailSenderExecutorService = Optional.absent();
    }
  }
  
  @Override
  public void close() {
    if (!mailSenderExecutorService.isPresent()) {
      return;
    }
    
    closer.shutdown(getClass().getName(), mailSenderExecutorService.get());
  }
  
  private String getEmailLogFormat(List<String> toList, String subject, String body) {
    return String.format("[to: %s, subject: %s, body: %s]", toList, subject, body);
  }
  
  public void sendTaskFailedMail(SingularityTaskId taskId, SingularityRequest request, TaskState state) {
    final List<String> to = request.getOwners();
    final String subject = String.format("Task %s failed with state %s", taskId.toString(), state.name());
    final String body = "Click here to view the logs";
    
    queueMail(to, subject, body);
  }
  
  private void queueMail(final List<String> toList, final String subject, final String body) {
    if (toList.isEmpty()) {
      LOG.warn(String.format("Couldn't queue email %s because no to address is present", getEmailLogFormat(toList, subject, body)));
    }
    
    if (!mailSenderExecutorService.isPresent()) {
      LOG.warn(String.format("Couldn't queue email %s because no SMTP configuration is present", getEmailLogFormat(toList, subject, body)));
    }  

    final Runnable cmd = new Runnable() {
      
      @Override
      public void run() {
        sendMail(toList, subject, body);
      }
    };
    
    mailSenderExecutorService.get().submit(cmd);
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
      LOG.warn(String.format("Unable to send message %s", getEmailLogFormat(toList, subject, body)), t);
    }
  }

}
