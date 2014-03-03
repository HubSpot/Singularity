package com.hubspot.singularity.smtp;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityCloseable;
import com.hubspot.singularity.SingularityCloser;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.SMTPConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.history.HistoryManager;
import com.hubspot.singularity.data.history.HistoryManager.OrderDirection;
import com.hubspot.singularity.data.history.HistoryManager.RequestHistoryOrderBy;
import com.hubspot.singularity.data.SandboxManager;
import com.hubspot.mesos.json.MesosFileChunkObject;
import com.ning.http.client.AsyncHttpClient;

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
  private JadeTemplate requestPausedTemplate;
  private JadeTemplate taskNotRunningWarningTemplate;

  private final AsyncHttpClient asyncHttpClient;
  private final ObjectMapper objectMapper;

  private final Optional<String> uiHostnameAndPath;

  private static final String TASK_LINK_FORMAT = "%s/task/%s";

  @Inject
  public SingularityMailer(SingularityConfiguration configuration, Optional<SMTPConfiguration> maybeSmtpConfiguration, SingularityCloser closer, HistoryManager historyManager, AsyncHttpClient asyncHttpClient, ObjectMapper objectMapper) {
    this.maybeSmtpConfiguration = maybeSmtpConfiguration;
    this.closer = closer;
    this.configuration = configuration;
    this.uiHostnameAndPath = configuration.getSingularityUIHostnameAndPath();
    this.historyManager = historyManager;
    this.asyncHttpClient = asyncHttpClient;
    this.objectMapper = objectMapper;

    if (maybeSmtpConfiguration.isPresent()) {
      this.mailSenderExecutorService = Optional.of(new ThreadPoolExecutor(maybeSmtpConfiguration.get().getMailThreads(), maybeSmtpConfiguration.get().getMailMaxThreads(), 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(),
          new ThreadFactoryBuilder().setNameFormat("SingularityMailer-%d").build()));
    } else {
      this.mailSenderExecutorService = Optional.absent();
    }

    try {
      this.taskFailedTemplate = Jade4J.getTemplate("./src/main/resources/templates/task_failed.jade");
      this.requestPausedTemplate = Jade4J.getTemplate("./src/main/resources/templates/request_paused.jade");
      this.taskNotRunningWarningTemplate = Jade4J.getTemplate("./src/main/resources/templates/task_not_running_warning.jade");
    } catch (IOException e) {
      Throwables.propagate(e);
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

  private Optional<String[]> getTaskLogFile(SingularityTaskId taskId, String filename) {
    final Optional<SingularityTaskHistory> maybeTaskHistory = historyManager.getTaskHistory(taskId.getId(), true);

    if (!maybeTaskHistory.isPresent()) {
      LOG.error("No task history found for %s. This shouldn't happen.", taskId.getId());
      return Optional.absent();
    }

    final SingularityTaskHistory taskHistory = maybeTaskHistory.get();

    final String slaveHostname = taskHistory.getTask().getOffer().getHostname();

    final String directory = String.format("%s/%s", taskHistory.getDirectory().get(), filename);

    final long logLength = this.maybeSmtpConfiguration.get().getTaskLogLength();

    final SandboxManager sandboxManager = new SandboxManager(this.asyncHttpClient, this.objectMapper);

    final Optional<MesosFileChunkObject> logChunkObject;
    try {
      logChunkObject = sandboxManager.read(slaveHostname, directory, Optional.of((long) 0), Optional.of((long) logLength));
    } catch (RuntimeException e) {
      LOG.error(String.format("Sanboxmanager failed to read %s/%s on slave %s with error %s", directory, filename, slaveHostname, e));
      return Optional.absent();
    }

    if (logChunkObject.isPresent()) {
      return Optional.of(logChunkObject.get().getData().split("[\r\n]+"));
    } else {
      LOG.error(String.format("Singularity mailer failed to get %s log for %s task ", filename, taskId.getId()));
      return Optional.absent();
    }
  }

  private String populateGenericEmailTemplate(JadeTemplate template, SingularityRequest request, SingularityTaskId taskId, Optional<SingularityTaskHistory> maybeTaskHistory, Optional<TaskState> taskState, Map<String, Object> additionalBindings){
    Builder<String, Object> templateSubs = ImmutableMap.<String, Object>builder();
    templateSubs.put("request_id", request.getId());
    templateSubs.put("singularity_task_link", getSingularityTaskLink(taskId));
    templateSubs.put("stdout", getTaskLogFile(taskId, "stdout").or(new String[0]));
    templateSubs.put("stderr", getTaskLogFile(taskId, "stderr").or(new String[0]));
    templateSubs.put("duration_left", DurationFormatUtils.formatDurationHMS(TimeUnit.SECONDS.toMillis(configuration.getKillAfterTasksDoNotRunDefaultSeconds())));
    templateSubs.put("task_id", taskId.getId());

    if(taskState.isPresent()){
      templateSubs.put("status", taskState.get().name());    
      templateSubs.put("taskStateLost", (taskState.get() == TaskState.TASK_LOST));
      templateSubs.put("taskStateFailed", (taskState.get() == TaskState.TASK_FAILED));
    }
    
    if (maybeTaskHistory.isPresent()) {
      final SingularityTaskHistory taskHistory = maybeTaskHistory.get();
      templateSubs.put("task_updates", taskHistory.getTaskHistoryJade());
      templateSubs.put("task_directory", taskHistory.getDirectory().or("directory missing"));
      templateSubs.put("slave_hostname", taskHistory.getTask().getOffer().getHostname());
      templateSubs.put("taskEverRan", taskEverRan(taskHistory));
    }
    
    
    for (Map.Entry<String, Object> bindingEntry : additionalBindings.entrySet()){
      templateSubs.put(bindingEntry.getKey(), bindingEntry.getValue());
    }
    
    return Jade4J.render(template, templateSubs.build());
  }
  
  public void sendTaskNotRunningWarningEmail(SingularityTaskId taskId, long duration, SingularityRequest request) {
    Optional<SingularityTaskHistory> maybeTaskHistory = historyManager.getTaskHistory(taskId.getId(), true);

    final List<String> to = request.getOwners();
    final String subject = String.format("Task %s has not started yet", taskId.getId());

    ImmutableMap<String, Object> additionalBindings = ImmutableMap.<String, Object>builder()
        .put("duration_running", DurationFormatUtils.formatDurationHMS(duration))
        .build();
  
    final String body = populateGenericEmailTemplate(this.taskNotRunningWarningTemplate, request, taskId, maybeTaskHistory, Optional.<TaskState>absent(), additionalBindings);
  
    queueMail(to, subject, body);
  }

  public void sendTaskFailedMail(SingularityTaskId taskId, SingularityRequest request, TaskState taskState) {
    Optional<SingularityTaskHistory> maybeTaskHistory = historyManager.getTaskHistory(taskId.getId(), true);

    final List<String> to = request.getOwners();
    final String subject = getSubjectForTaskHistory(taskId, taskState, maybeTaskHistory);

    Joiner joiner = Joiner.on(", ").skipNulls();
    final String adminEmails = joiner.join(this.maybeSmtpConfiguration.get().getAdmins());
    
    ImmutableMap<String, Object> additionalBindings = ImmutableMap.<String, Object>builder()
        .put("adminEmails", adminEmails.toString())
        .build();
    
    final String body = populateGenericEmailTemplate(this.taskFailedTemplate, request, taskId, maybeTaskHistory, Optional.of(taskState), additionalBindings);

    queueMail(to, subject, body);
  }
  

  public void sendRequestPausedMail(SingularityTaskId taskId, SingularityRequest request) {
    Optional<SingularityTaskHistory> maybeTaskHistory = historyManager.getTaskHistory(taskId.getId(), true);

    final int maxFailures = request.getMaxFailuresBeforePausing();

    final List<SingularityRequestHistory> requestHistories = historyManager.getRequestHistory(request.getId(), Optional.of(RequestHistoryOrderBy.createdAt), Optional.of(OrderDirection.DESC), 0, maxFailures);
    final List<Map<String, String>> requestHistoryFormatted = Lists.newArrayList();

    for (SingularityRequestHistory requestHistory : requestHistories) {
      requestHistoryFormatted.add(requestHistory.formatJadeJson());
    }

    final List<String> to = request.getOwners();
    final String subject = String.format("Request %s is PAUSED", request.getId());

    ImmutableMap<String, Object> additionalBindings = ImmutableMap.<String, Object>builder()
        .put("num_failures", maxFailures)
        .put("request_history", requestHistoryFormatted)
        .build();

    final String body = populateGenericEmailTemplate(this.requestPausedTemplate, request, taskId, maybeTaskHistory, Optional.<TaskState>absent(), additionalBindings);

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

