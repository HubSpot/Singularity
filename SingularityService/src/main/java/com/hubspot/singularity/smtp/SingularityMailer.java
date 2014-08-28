package com.hubspot.singularity.smtp;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.json.MesosFileChunkObject;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.SingularityCloseable;
import com.hubspot.singularity.SingularityCloser;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityServiceModule;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskHistoryUpdate.SimplifiedTaskState;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.SMTPConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.SandboxManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;
import com.ning.http.client.AsyncHttpClient;

import de.neuland.jade4j.Jade4J;
import de.neuland.jade4j.template.JadeTemplate;

public class SingularityMailer implements SingularityCloseable {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityMailer.class);

  private final SingularityConfiguration configuration;
  private final Optional<SMTPConfiguration> maybeSmtpConfiguration;
  private final Optional<ThreadPoolExecutor> mailSenderExecutorService;

  private final SingularityCloser closer;

  private final TaskManager taskManager;

  private final JadeTemplate taskFailedTemplate;
  private final JadeTemplate requestInCooldownTemplate;
  private final JadeTemplate taskNotRunningWarningTemplate;

  private final JadeHelper jadeHelper;

  private final AsyncHttpClient asyncHttpClient;
  private final ObjectMapper objectMapper;

  private final Optional<String> uiHostnameAndPath;

  private final SingularityExceptionNotifier exceptionNotifier;

  private static final String TASK_LINK_FORMAT = "%s/task/%s";
  private static final String REQUEST_LINK_FORMAT = "%s/request/%s";

  @Inject
  public SingularityMailer(SingularityConfiguration configuration, Optional<SMTPConfiguration> maybeSmtpConfiguration, JadeHelper jadeHelper, SingularityCloser closer, TaskManager taskManager, AsyncHttpClient asyncHttpClient,
      ObjectMapper objectMapper, @Named(SingularityServiceModule.TASK_FAILED_TEMPLATE) JadeTemplate taskFailedTemplate, @Named(SingularityServiceModule.REQUEST_IN_COOLDOWN_TEMPLATE) JadeTemplate requestInCooldownTemplate,
      @Named(SingularityServiceModule.TASK_NOT_RUNNING_WARNING_TEMPLATE) JadeTemplate taskNotRunningWarningTemplate, SingularityExceptionNotifier exceptionNotifier) {
    this.maybeSmtpConfiguration = maybeSmtpConfiguration;
    this.closer = closer;
    this.jadeHelper = jadeHelper;
    this.configuration = configuration;
    this.uiHostnameAndPath = configuration.getUiConfiguration().getBaseUrl().or(configuration.getSingularityUIHostnameAndPath());
    this.taskManager = taskManager;
    this.asyncHttpClient = asyncHttpClient;
    this.objectMapper = objectMapper;

    if (maybeSmtpConfiguration.isPresent()) {
      this.mailSenderExecutorService = Optional.of(
          new ThreadPoolExecutor(maybeSmtpConfiguration.get().getMailThreads(), maybeSmtpConfiguration.get().getMailMaxThreads(), 0L, TimeUnit.MILLISECONDS,
              new LinkedBlockingQueue<Runnable>(),
              new ThreadFactoryBuilder().setNameFormat("SingularityMailer-%d")
          .build()));
    } else {
      this.mailSenderExecutorService = Optional.absent();
    }

    this.taskFailedTemplate = taskFailedTemplate;
    this.requestInCooldownTemplate = requestInCooldownTemplate;
    this.taskNotRunningWarningTemplate = taskNotRunningWarningTemplate;

    this.exceptionNotifier = exceptionNotifier;
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

  private String getEmailLogFormat(List<String> toList, String subject) {
    return String.format("[to: %s, subject: %s]", toList, subject);
  }

  private Optional<String[]> getTaskLogFile(SingularityTaskId taskId, String filename) {
    final Optional<SingularityTask> task = taskManager.getTask(taskId);

    if (!task.isPresent()) {
      LOG.error(String.format("No task found for %s", taskId.getId()));
      return Optional.absent();
    }

    final Optional<String> directory = taskManager.getDirectory(taskId);

    if (!directory.isPresent()) {
      LOG.error(String.format("No directory found for task %s to fetch logs", taskId));
      return Optional.absent();
    }

    final String slaveHostname = task.get().getOffer().getHostname();

    final String fullPath = String.format("%s/%s", directory.get(), filename);

    final Long logLength = new Long(this.maybeSmtpConfiguration.get().getTaskLogLength());

    final SandboxManager sandboxManager = new SandboxManager(this.asyncHttpClient, this.objectMapper);

    final Optional<MesosFileChunkObject> logChunkObject;

    try {
      logChunkObject = sandboxManager.read(slaveHostname, fullPath, Optional.of(0L), Optional.of(logLength));
    } catch (RuntimeException e) {
      LOG.error(String.format("Sandboxmanager failed to read %s/%s on slave %s", directory, filename, slaveHostname),  e);
      return Optional.absent();
    }

    if (logChunkObject.isPresent()) {
      return Optional.of(logChunkObject.get().getData().split("[\r\n]+"));
    } else {
      LOG.error(String.format("Singularity mailer failed to get %s log for %s task ", filename, taskId.getId()));
      return Optional.absent();
    }
  }

  private String populateGenericEmailTemplate(JadeTemplate template, SingularityRequest request, Optional<SingularityTaskId> taskId, Collection<SingularityTaskHistoryUpdate> taskHistory, Optional<ExtendedTaskState> taskState,
      Map<String, Object> additionalBindings) {
    Builder<String, Object> templateSubs = ImmutableMap.<String, Object> builder();

    templateSubs.put("request_id", request.getId());
    templateSubs.put("singularity_request_link", getSingularityRequestLink(request));

    if (taskId.isPresent()) {
      templateSubs.put("singularity_task_link", getSingularityTaskLink(taskId.get()));
      templateSubs.put("stdout", getTaskLogFile(taskId.get(), "stdout").or(new String[0]));
      templateSubs.put("stderr", getTaskLogFile(taskId.get(), "stderr").or(new String[0]));
      templateSubs.put("duration_left", DurationFormatUtils.formatDurationHMS(TimeUnit.SECONDS.toMillis(configuration.getKillAfterTasksDoNotRunDefaultSeconds())));
      templateSubs.put("task_id", taskId.get().getId());
      templateSubs.put("deploy_id", taskId.get().getDeployId());

      templateSubs.put("task_directory", taskManager.getDirectory(taskId.get()).or("directory missing"));

      Optional<SingularityTask> task = taskManager.getTask(taskId.get());

      if (task.isPresent()) {
        templateSubs.put("slave_hostname", task.get().getOffer().getHostname());
      }
    }

    templateSubs.put("taskScheduled", request.isScheduled());

    templateSubs.put("taskWillRetry", request.getNumRetriesOnFailure().or(0) > 0);
    templateSubs.put("num_retries", request.getNumRetriesOnFailure().or(0));

    if (taskState.isPresent()) {
      templateSubs.put("status", taskState.get().getDisplayName());
      templateSubs.put("taskStateLost", (taskState.get() == ExtendedTaskState.TASK_LOST || taskState.get() == ExtendedTaskState.TASK_LOST_WHILE_DOWN));
      templateSubs.put("taskStateFailed", (taskState.get() == ExtendedTaskState.TASK_FAILED));
    }

    templateSubs.put("task_updates", jadeHelper.getJadeTaskHistory(taskHistory));
    templateSubs.put("taskEverRan", taskEverRan(taskHistory));

    for (Map.Entry<String, Object> bindingEntry : additionalBindings.entrySet()) {
      templateSubs.put(bindingEntry.getKey(), bindingEntry.getValue());
    }

    return Jade4J.render(template, templateSubs.build());
  }

  private List<String> getOwners(SingularityRequest request) {
    return request.getOwners().or(Collections.<String> emptyList());
  }

  public void sendTaskNotRunningWarningEmail(SingularityTaskId taskId, long duration, SingularityRequest request) {
    Collection<SingularityTaskHistoryUpdate> taskHistory = taskManager.getTaskHistoryUpdates(taskId);

    final List<String> to = getOwners(request);
    final String subject = String.format("Task has not started yet - %s", taskId.getId());

    ImmutableMap<String, Object> additionalBindings = ImmutableMap.<String, Object> builder()
        .put("duration_running", DurationFormatUtils.formatDurationHMS(duration))
        .build();

    final String body = populateGenericEmailTemplate(this.taskNotRunningWarningTemplate, request, Optional.of(taskId), taskHistory, Optional.<ExtendedTaskState> absent(), additionalBindings);

    queueMail(to, subject, body);
  }

  public void sendTaskFailedMail(SingularityTaskId taskId, SingularityRequest request, ExtendedTaskState taskState) {
    if (maybeSmtpConfiguration.isPresent()) {
      Collection<SingularityTaskHistoryUpdate> taskHistory = taskManager.getTaskHistoryUpdates(taskId);

      final List<String> to = getOwners(request);
      final String subject = getSubjectForTaskHistory(taskId, taskState, taskHistory);

      Joiner joiner = Joiner.on(", ").skipNulls();
      final String adminEmails = joiner.join(maybeSmtpConfiguration.get().getAdmins());

      ImmutableMap<String, Object> additionalBindings = ImmutableMap.<String, Object>builder()
          .put("adminEmails", adminEmails.toString())
          .build();

      final String body = populateGenericEmailTemplate(taskFailedTemplate, request, Optional.of(taskId), taskHistory, Optional.of(taskState), additionalBindings);

      queueMail(to, subject, body);
    }
  }

  public void sendRequestInCooldownMail(SingularityRequest request) {
    // should have task history

    final List<String> to = getOwners(request);
    final String subject = String.format("Request %s has entered system cooldown — Singularity", request.getId());

    ImmutableMap<String, Object> additionalBindings = ImmutableMap.<String, Object> builder()
        .put("num_failures", configuration.getCooldownAfterFailures())
        .put("cooldown_delay_format", DurationFormatUtils.formatDurationHMS(TimeUnit.SECONDS.toMillis(configuration.getCooldownMinScheduleSeconds())))
        .put("cooldown_expires_format", DurationFormatUtils.formatDurationHMS(TimeUnit.MINUTES.toMillis(configuration.getCooldownExpiresAfterMinutes())))
        .build();

    final String body = populateGenericEmailTemplate(this.requestInCooldownTemplate, request, Optional.<SingularityTaskId> absent(), Collections.<SingularityTaskHistoryUpdate> emptyList(),
        Optional.<ExtendedTaskState> absent(), additionalBindings);

    queueMail(to, subject, body);
  }

  private boolean taskEverRan(Collection<SingularityTaskHistoryUpdate> history) {
    SimplifiedTaskState simplifiedTaskState = SingularityTaskHistoryUpdate.getCurrentState(history);

    return simplifiedTaskState == SimplifiedTaskState.DONE || simplifiedTaskState == SimplifiedTaskState.RUNNING;
  }

  private String getSubjectForTaskHistory(SingularityTaskId taskId, ExtendedTaskState state, Collection<SingularityTaskHistoryUpdate> history) {
    if (!taskEverRan(history)) {
      return String.format("Task %s, never started: (%s)", state.getDisplayName(), taskId.toString());
    }

    return String.format("Task %s after running: (%s)", state.getDisplayName(), taskId.toString());
  }

  private String getSingularityTaskLink(SingularityTaskId taskId) {
    if (!uiHostnameAndPath.isPresent()) {
      return "";
    }

    return String.format(TASK_LINK_FORMAT, uiHostnameAndPath.get(), taskId.getId());
  }

  private String getSingularityRequestLink(SingularityRequest request) {
    if (!uiHostnameAndPath.isPresent()) {
      return "";
    }

    return String.format(REQUEST_LINK_FORMAT, uiHostnameAndPath.get(), request.getId());
  }

  private void queueMail(final List<String> toList, final String subject, final String body) {
    if (toList.isEmpty()) {
      LOG.warn(String.format("Couldn't queue email %s because no to address is present", getEmailLogFormat(toList, subject)));
      return;
    }

    if (!mailSenderExecutorService.isPresent()) {
      LOG.warn(String.format("Couldn't queue email %s because no SMTP configuration is present", getEmailLogFormat(toList, subject)));
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
      LOG.warn(String.format("Couldn't send email %s because no SMTP configuration is present", getEmailLogFormat(toList, subject)));
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

      LOG.trace(String.format("Sending a message to %s - %s", Arrays.toString(toArray), getEmailLogFormat(toList, subject)));

      Transport.send(message);
    } catch (Throwable t) {
      LOG.warn(String.format("Unable to send message %s", getEmailLogFormat(toList, subject)), t);
      exceptionNotifier.notify(t);
    }
  }

  private Address[] getAddresses(List<String> toList) {
    List<InternetAddress> addresses = Lists.newArrayListWithCapacity(toList.size());

    for (String to : toList) {
      try {
        addresses.add(new InternetAddress(to));
      } catch (AddressException t) {
        LOG.warn(String.format("Invalid address %s - ignoring", to), t);
      }
    }

    return addresses.toArray(new InternetAddress[addresses.size()]);
  }
}
