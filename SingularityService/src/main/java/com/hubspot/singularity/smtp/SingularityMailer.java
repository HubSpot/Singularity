package com.hubspot.singularity.smtp;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.*;
import com.hubspot.singularity.config.SMTPConfiguration;
import com.hubspot.singularity.data.StateManager;
import com.hubspot.singularity.data.history.HistoryManager;
import org.apache.mesos.Protos.TaskState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SingularityMailer implements SingularityCloseable {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityMailer.class);

  private final Optional<SMTPConfiguration> maybeSmtpConfiguration;
  private final Optional<ThreadPoolExecutor> mailSenderExecutorService;
  
  private final SingularityCloser closer;

  private final StateManager stateManager;
  private final HistoryManager historyManager;

  private final SimpleEmailTemplate taskFailedTemplate;

  private static final String LOGS_LINK_FORMAT = "http://%s:5050/#/slaves/%s/browse?path=%s";
  
  @Inject
  public SingularityMailer(Optional<SMTPConfiguration> maybeSmtpConfiguration, SingularityCloser closer, StateManager stateManager, HistoryManager historyManager) {
    this.maybeSmtpConfiguration = maybeSmtpConfiguration;
    this.closer = closer;
    this.stateManager = stateManager;
    this.historyManager = historyManager;
    
    if (maybeSmtpConfiguration.isPresent()) {
      this.mailSenderExecutorService = Optional.of(new ThreadPoolExecutor(maybeSmtpConfiguration.get().getMailThreads(), maybeSmtpConfiguration.get().getMailMaxThreads(), 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactoryBuilder().setNameFormat("SingularityMailer-%d").build()));
    } else {
      this.mailSenderExecutorService = Optional.absent();
    }
    
    this.taskFailedTemplate = new SimpleEmailTemplate("task_failed.html");
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
  
  public void sendTaskFailedMail(SingularityTaskId taskId, SingularityRequest request, TaskState state) {
    final List<String> to = request.getOwners();
    
    Optional<SingularityTaskHistory> taskHistory = historyManager.getTaskHistory(taskId.getId(), true);
    
    final String subject = getSubjectForTaskHistory(taskId, state, taskHistory);
    
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    builder.put("request_id", request.getId());
    builder.put("task_id", taskId.getId());
    builder.put("status", state.name());
    builder.put("mesos_logs_link", getMesosLogsLink(taskHistory));
    
    final String body = taskFailedTemplate.render(builder.build());
    
    queueMail(to, subject, body);
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
      return String.format("Task %s never started in mesos (%s)", taskId.toString(), state.name());
    }
    
    return String.format("Task %s failed after running (%s)", taskId.toString(), state.name());
  }
  
  private String getMesosLogsLink(Optional<SingularityTaskHistory> taskHistory) {
    if (!taskHistory.isPresent() || !taskHistory.get().getDirectory().isPresent()) {
      return "";
    }
    
    String masterHost = null;
    
    for (SingularityHostState state : stateManager.getHostStates()) {
      if (state.isMaster()) {
        masterHost = state.getHostname();
      }
    }
    
    if (masterHost == null) {
      return "";
    }
    
    String slave = taskHistory.get().getTask().getOffer().getSlaveId().getValue();
    
    return String.format(LOGS_LINK_FORMAT, masterHost, slave, JavaUtils.urlEncode(taskHistory.get().getDirectory().get()));
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

      final Transport transport = session.getTransport("smtp");
      
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

      transport.send(message);
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
