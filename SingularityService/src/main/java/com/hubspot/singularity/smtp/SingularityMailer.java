package com.hubspot.singularity.smtp;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
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

import com.hubspot.connect.http.HttpClient;
import com.hubspot.connect.http.HttpHelper;
import com.hubspot.connect.http.HttpRequest;
import com.hubspot.connect.http.HttpRequest.HttpMethod;
import com.hubspot.connect.http.HttpResponse;

import de.neuland.jade4j.Jade4J;
import de.neuland.jade4j.template.JadeTemplate;

public class SingularityMailer implements SingularityCloseable {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityMailer.class);

  private final SingularityConfiguration configuration;
  private final Optional<SMTPConfiguration> maybeSmtpConfiguration;
  private final Optional<ThreadPoolExecutor> mailSenderExecutorService;
  
  private final SingularityCloser closer;
  
  private final HttpClient httpClient = HttpHelper.createDefaultClient();

  private final HistoryManager historyManager;
  
  private JadeTemplate taskFailedTemplate;
  private JadeTemplate requestPausedTemplate;
  private JadeTemplate taskNotRunningWarningTemplate;
  
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
      this.requestPausedTemplate = Jade4J.getTemplate("./src/main/resources/templates/request_paused.jade");
      this.taskNotRunningWarningTemplate = Jade4J.getTemplate("./src/main/resources/templates/task_not_running_warning.jade");
    } catch(IOException e) {
      LOG.error("SingularityMailer: task failed template not found: " + e);
      this.requestPausedTemplate = new JadeTemplate();
      this.taskFailedTemplate = new JadeTemplate();
      this.taskNotRunningWarningTemplate = new JadeTemplate();
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
  
  public Map<String, List<String>> getTaskStdOutErr(SingularityTaskId taskId) {
    Optional<SingularityTaskHistory> maybeTaskHistory = historyManager.getTaskHistory(taskId.getId(), true);
    final SingularityTaskHistory taskHistory = maybeTaskHistory.get();
  
    final String directory = taskHistory.getDirectory().get();
    final String slave_hostname_suffix = this.maybeSmtpConfiguration.get().getSlaveHostnameSuffix();
    final String slave_hostname = (taskHistory.getTask().getOffer().getHostname()  + slave_hostname_suffix);
    
    Map<String, List<String>> stdouterr = new HashMap<String, List<String>>();
    stdouterr.put("stdout", Collections.<String>emptyList());
    stdouterr.put("stderr", Collections.<String>emptyList());    
    
    for( String file : Arrays.asList("stdout", "stderr") ) {
      try {
        HttpResponse response = this.httpClient.execute(HttpRequest.newBuilder()
            .setUri(slave_hostname + ":5051/files/read.json")
            .addQueryParam("path", directory + "/" + file)
            .addQueryParam("offset", "0")
            .addQueryParam("length", this.maybeSmtpConfiguration.get().getTaskLogLength())
            .setMethod(HttpMethod.POST)
            .build());
        
        if(response.isError()){
          LOG.error("Singularity mailer couldn't GET file " + file);
        } else {
          String data = response.getAsJsonNode().get("data").toString();
          if(data != null && data.length() > 0){
            stdouterr.put(file, Arrays.asList( data.split("[\r\n]+")) );
          }
        }
      } catch(Exception e){
        LOG.error("Singularity mailer exception: " + e);
      }
    }
    return stdouterr;
  }
  
  public void sendRequestPausedMail(SingularityTaskId taskId, SingularityRequest request) {
    Optional<SingularityTaskHistory> maybeTaskHistory = historyManager.getTaskHistory(taskId.getId(), true);
    
    final SingularityTaskHistory taskHistory = maybeTaskHistory.get();
    
    final List<String> to = request.getOwners();
    final String subject = String.format("Request %s is PAUSED", request.getId());
    
    Map<String, Object> templateSubs = new HashMap<String, Object>();
    templateSubs.put("request_id", request.getId());
    templateSubs.put("num_failures", request.getMaxFailuresBeforePausing());
    templateSubs.put("task_updates", taskHistory.getTaskHistoryJade());
    final String body = Jade4J.render(this.requestPausedTemplate,  templateSubs);   
    
    queueMail(to, subject, body); 
  }
    
  public void sendTaskNotRunningWarningEmail(SingularityTaskId taskId, long duration, SingularityRequest request) {
    Optional<SingularityTaskHistory> maybeTaskHistory = historyManager.getTaskHistory(taskId.getId(), true);
    
    final SingularityTaskHistory taskHistory = maybeTaskHistory.get();
    
    final List<String> to = request.getOwners();
 
    final String subject = String.format("Task %s has not started yet", taskId.getId());
    
    Map<String, Object> templateSubs = new HashMap<String, Object>();
    
    templateSubs.put("request_id", request.getId());
    templateSubs.put("task_id", taskId.getId());
    templateSubs.put("duration_running", DurationFormatUtils.formatDurationHMS(duration));
    templateSubs.put("duration_left", DurationFormatUtils.formatDurationHMS(TimeUnit.SECONDS.toMillis(configuration.getKillAfterTasksDoNotRunDefaultSeconds())));
    templateSubs.put("singularity_task_link", getSingularityTaskLink(taskId));
    templateSubs.put("task_updates", taskHistory.getTaskHistoryJade());
    
    final String body = Jade4J.render(this.taskNotRunningWarningTemplate,  templateSubs);
    
    queueMail(to, subject, body);
}
    
  public void sendTaskFailedMail(SingularityTaskId taskId, SingularityRequest request, TaskState taskState){
    Optional<SingularityTaskHistory> maybeTaskHistory = historyManager.getTaskHistory(taskId.getId(), true);
    
    final List<String> to = request.getOwners();
    final String subject = getSubjectForTaskHistory(taskId, taskState, maybeTaskHistory);
    
    final StringBuilder adminEmails = new StringBuilder();
    for (String s : this.maybeSmtpConfiguration.get().getAdmins()) {
      adminEmails.append(s);
      adminEmails.append(",");
    }
       
    final SingularityTaskHistory taskHistory = maybeTaskHistory.get();
    
    Map<String, Object> templateSubs = new HashMap<String, Object>();
    
    Map<String, List<String>> stdouterr = getTaskStdOutErr(taskId);
    
    //logic
    templateSubs.put("taskStateLost", (taskState == TaskState.TASK_LOST));
    templateSubs.put("taskStateFailed", (taskState == TaskState.TASK_FAILED));
    templateSubs.put("taskEverRan", taskEverRan(taskHistory));
    //display 
    templateSubs.put("request_id", request.getId());
    templateSubs.put("task_id", taskId.getId());
    templateSubs.put("status", taskState.name());
    templateSubs.put("singularity_task_link", getSingularityTaskLink(taskId)); 
    templateSubs.put("task_directory", taskHistory.getDirectory().get());
    templateSubs.put("slave_hostname", (taskHistory.getTask().getOffer().getHostname()  + ".hubspot.com"));
    templateSubs.put("task_updates", taskHistory.getTaskHistoryJade());
    templateSubs.put("adminEmails", adminEmails.toString());
    templateSubs.put("stdout", stdouterr.get("stdout"));
    templateSubs.put("stderr", stdouterr.get("stderr"));
    
    final String body = Jade4J.render(this.taskFailedTemplate,  templateSubs);
    
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
