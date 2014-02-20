package com.hubspot.singularity.smtp;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.mail.Address;
import javax.mail.Message.RecipientType;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.mesos.Protos.TaskState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityCloseable;
import com.hubspot.singularity.SingularityCloser;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.SMTPConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.history.HistoryManager;

import de.neuland.jade4j.Jade4J;
import de.neuland.jade4j.template.JadeTemplate;

public class SingularityMailer implements SingularityCloseable {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityMailer.class);

  private final SingularityConfiguration configuration;
  private final Optional<SMTPConfiguration> maybeSmtpConfiguration;
  private final Optional<ThreadPoolExecutor> mailSenderExecutorService;
  
  private final SingularityCloser closer;

  private final HistoryManager historyManager;
  
  private JadeTemplate taskFailedTemplate;
  
  private final Optional<String> uiHostnameAndPath;

  private static final String TASK_LINK_FORMAT = "%s/task/%s";
  
  @Inject
  public SingularityMailer(SingularityConfiguration configuration, Optional<SMTPConfiguration> maybeSmtpConfiguration, SingularityCloser closer, HistoryManager historyManager) {
    this.maybeSmtpConfiguration = maybeSmtpConfiguration;
    this.closer = closer;
    this.configuration = configuration;
    this.uiHostnameAndPath = configuration.getSingularityUIHostnameAndPath();
    this.historyManager = historyManager;
    
    if (maybeSmtpConfiguration.isPresent()) {
      this.mailSenderExecutorService = Optional.of(new ThreadPoolExecutor(maybeSmtpConfiguration.get().getMailThreads(), maybeSmtpConfiguration.get().getMailMaxThreads(), 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactoryBuilder().setNameFormat("SingularityMailer-%d").build()));
    } else {
      this.mailSenderExecutorService = Optional.absent();
    }
    
    try{
      this.taskFailedTemplate = Jade4J.getTemplate("./src/main/resources/templates/task_failed.jade");
    } catch(IOException e) {
      LOG.error("SingularityMailer: task failed template not found: " + e);
      this.taskFailedTemplate = new JadeTemplate();
    }
  }
  
  @Override
  public void close() {
    if (!mailSenderExecutorService.isPresent()) {
      return;
    }
    
    closer.shutdown(getClass().getName(), mailSenderExecutorService.get());
  }
  
  public void sendAbortMail() {
    if (!maybeSmtpConfiguration.isPresent()) {
      LOG.warn("Couldn't send abort mail because no SMTP configuration is present");
      return;
    }
    
    final List<String> to = maybeSmtpConfiguration.get().getAdmins();
    final String subject = String.format("Singularity on %s is aborting", JavaUtils.getHostName());
    
    queueMail(to, subject, "");
  }
  
  private String getEmailLogFormat(List<String> toList, String subject, String body) {
    return String.format("[to: %s, subject: %s, body: %s]", toList, subject, body);
  }
  
  public void sendRequestPausedMail(SingularityRequest request) {
    final List<String> to = request.getOwners();
    final String subject = String.format("Request %s is PAUSED", request.getId());
    final String body = String.format("It has failed %s times consecutively. It will not run again until it is manually unpaused or updated.", request.getMaxFailuresBeforePausing());
    
    queueMail(to, subject, body); 
  }
  
  public void sendTaskNotRunningWarningEmail(SingularityTaskId taskId, long duration, SingularityRequest request) {
    final List<String> to = request.getOwners();
    
    final String subject = String.format("Task %s has not started yet", taskId.getId());
    
    // should have a nicer message, task history and links
    final String body = String.format("It has been running for %s. It will be killed after %s and a new one will take its place", DurationFormatUtils.formatDurationHMS(duration), DurationFormatUtils.formatDurationHMS(TimeUnit.SECONDS.toMillis(configuration.getKillAfterTasksDoNotRunDefaultSeconds())));
    
    queueMail(to, subject, body);
}
    
    
  public void sendTaskFailedMail(SingularityTaskId taskId, SingularityRequest request, TaskState state) {
    Optional<SingularityTaskHistory> taskHistory = historyManager.getTaskHistory(taskId.getId(), true);
    
    final List<String> to = request.getOwners();
    final String subject = getSubjectForTaskHistory(taskId, state, taskHistory);
    final String emailBody = buildTaskFailedEmail(state, request, taskId, taskHistory);
      
    queueMail(to, subject, emailBody);
  }
  
  private boolean taskEverRan(SingularityTaskHistory taskHistory) {
    for (SingularityTaskHistoryUpdate update : taskHistory.getTaskUpdates()) {
      if (TaskState.valueOf(update.getStatusUpdate()) == TaskState.TASK_RUNNING) {
        return true;
      }
    }
    
    return false;
  }
  
  private String getSubjectForTaskHistory(SingularityTaskId taskId, TaskState state, Optional<SingularityTaskHistory> taskHistory) {
    if (!taskHistory.isPresent() || !taskEverRan(taskHistory.get())) {
      return String.format("(%s) - Task %s never started in mesos", state.name(), taskId.toString());
    }
    
    return String.format("(%s) - Task %s failed after running", state.name(), taskId.toString());
  }
  
  private String getSingularityTaskLink(SingularityTaskId taskId) {
    if (!uiHostnameAndPath.isPresent()) {
      return "";
    }
    
    return String.format(TASK_LINK_FORMAT, uiHostnameAndPath.get(), taskId.getId());
  }
  
  private void queueMail(final List<String> toList, final String subject, final String body) {
    if (toList.isEmpty()) {
      LOG.warn(String.format("Couldn't queue email %s because no to address is present", getEmailLogFormat(toList, subject, body)));
      return;
    }
    
    if (!mailSenderExecutorService.isPresent()) {
      LOG.warn(String.format("Couldn't queue email %s because no SMTP configuration is present", getEmailLogFormat(toList, subject, body)));
      return;
    }  
    
    final Runnable cmd = new Runnable() {
      
      @Override
      public void run() {
        sendMail(toList, subject, body);
      }
    };
    
    LOG.debug(String.format("Queuing an email to %s (subject: %s)", toList, subject)); 
    
    mailSenderExecutorService.get().submit(cmd);
  }

  private Session createSession(SMTPConfiguration smtpConfiguration, boolean useAuth) {
    Properties properties = System.getProperties();

    properties.setProperty("mail.smtp.host", smtpConfiguration.getHost());

    if (smtpConfiguration.getPort().isPresent()) {
      properties.setProperty("mail.smtp.port", Integer.toString(smtpConfiguration.getPort().get()));
    }

    if (useAuth) {
      properties.setProperty("mail.smtp.auth", "true");
      return Session.getInstance(properties, new SMTPAuthenticator(smtpConfiguration.getUsername().get(), smtpConfiguration.getPassword().get()));
    } else {
      return Session.getInstance(properties);
    }
  }
  
  public void sendMail(List<String> toList, String subject, String body) {
    if (!maybeSmtpConfiguration.isPresent()) {
      LOG.warn(String.format("Couldn't send email %s because no SMTP configuration is present", getEmailLogFormat(toList, subject, body)));
    }
    
    final SMTPConfiguration smtpConfiguration = maybeSmtpConfiguration.get();
    
    boolean useAuth = false;
    
    if (smtpConfiguration.getUsername().isPresent() && smtpConfiguration.getPassword().isPresent()) {
      useAuth = true;
    } else if (smtpConfiguration.getUsername().isPresent() || smtpConfiguration.getPassword().isPresent()) {
      LOG.warn(String.format("Not using smtp authentication because username (%s) or password (%s) was not present", smtpConfiguration.getUsername().isPresent(), smtpConfiguration.getPassword().isPresent()));
    }
    
    try {
      final Session session = createSession(maybeSmtpConfiguration.get(), useAuth);

      MimeMessage message = new MimeMessage(session);

      message.setFrom(new InternetAddress(smtpConfiguration.getFrom()));
      
      if (maybeSmtpConfiguration.get().isIncludeAdminsOnAllMails()) {
        Address[] ccArray = getAddresses(maybeSmtpConfiguration.get().getAdmins());
        if (ccArray.length > 0) {
          LOG.trace(String.format("Adding admins %s to mail %s", Arrays.toString(ccArray), subject));
          message.addRecipients(RecipientType.CC, ccArray);
        }
      }
      
      message.setSubject(subject);
      message.setContent(body, "text/html; charset=utf-8");
      
      Address[] toArray = getAddresses(toList);
      
      message.addRecipients(RecipientType.TO, toArray);
      
      LOG.trace(String.format("Sending a message to %s - %s", Arrays.toString(toArray), message));

      Transport.send(message);
    } catch (Throwable t) {
      LOG.warn(String.format("Unable to send message %s", getEmailLogFormat(toList, subject, body)), t);
    }
  }

  private Address[] getAddresses(List<String> toList) {
    List<InternetAddress> addresses = Lists.newArrayListWithCapacity(toList.size());
    
    for (String to : toList) {
      try {
        addresses.add(new InternetAddress(to));
      } catch (Throwable t) {
        LOG.warn(String.format("Invalid address %s - ignoring", to), t);
      }
    }
    
    return addresses.toArray(new InternetAddress[addresses.size()]);
  }
}
