package com.hubspot.singularity.smtp;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.SingularityCloseable;
import com.hubspot.singularity.SingularityCloser;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityServiceModule;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup.TaskCleanupType;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskHistoryUpdate.SimplifiedTaskState;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.EmailConfigurationEnums.EmailDestination;
import com.hubspot.singularity.config.EmailConfigurationEnums.EmailType;
import com.hubspot.singularity.config.SMTPConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;
import com.ning.http.client.AsyncHttpClient;

import de.neuland.jade4j.Jade4J;
import de.neuland.jade4j.template.JadeTemplate;

public class SingularityMailer implements SingularityCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityMailer.class);

  private final SingularityConfiguration configuration;
  private final Optional<SMTPConfiguration> maybeSmtpConfiguration;
  private final Optional<ThreadPoolExecutor> mailSenderExecutorService;

  private final SingularityCloser closer;

  private final TaskManager taskManager;

  private final JadeTemplate taskCompletedTemplate;
  private final JadeTemplate requestInCooldownTemplate;
  private final JadeTemplate requestModifiedTemplate;

  private final Optional<String> uiHostnameAndPath;

  private final Joiner adminJoiner;

  private final SingularityExceptionNotifier exceptionNotifier;

  private static final String TASK_LINK_FORMAT = "%s/task/%s";
  private static final String REQUEST_LINK_FORMAT = "%s/request/%s";

  @Inject
  public SingularityMailer(SingularityConfiguration configuration, Optional<SMTPConfiguration> maybeSmtpConfiguration, SingularityCloser closer, TaskManager taskManager, AsyncHttpClient asyncHttpClient,
      ObjectMapper objectMapper, @Named(SingularityServiceModule.TASK_COMPLETED_TEMPLATE) JadeTemplate taskCompletedTemplate, @Named(SingularityServiceModule.REQUEST_IN_COOLDOWN_TEMPLATE) JadeTemplate requestInCooldownTemplate,
      @Named(SingularityServiceModule.REQUEST_MODIFIED_TEMPLATE) JadeTemplate requestModifiedTemplate, SingularityExceptionNotifier exceptionNotifier) {
    this.maybeSmtpConfiguration = maybeSmtpConfiguration;
    this.closer = closer;
    this.configuration = configuration;
    this.uiHostnameAndPath = configuration.getUiConfiguration().getBaseUrl();
    this.taskManager = taskManager;
    this.adminJoiner = Joiner.on(", ").skipNulls();

    if (maybeSmtpConfiguration.isPresent()) {
      mailSenderExecutorService = Optional.of(
          new ThreadPoolExecutor(maybeSmtpConfiguration.get().getMailThreads(), maybeSmtpConfiguration.get().getMailMaxThreads(), 0L, TimeUnit.MILLISECONDS,
              new LinkedBlockingQueue<Runnable>(),
              new ThreadFactoryBuilder().setNameFormat("SingularityMailer-%d")
              .build()));
    } else {
      mailSenderExecutorService = Optional.absent();
    }

    this.requestModifiedTemplate = requestModifiedTemplate;
    this.taskCompletedTemplate = taskCompletedTemplate;
    this.requestInCooldownTemplate = requestInCooldownTemplate;

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
      LOG.debug("Not sending abort mail - no SMTP configuration is present");
      return;
    }

    final String subject = String.format("Singularity on %s is aborting", JavaUtils.getHostName());

    queueMail(getDestination(EmailType.SINGULARITY_ABORTING), Optional.<SingularityRequest> absent(), subject, "");
  }

  private String getEmailLogFormat(List<String> toList, String subject) {
    return String.format("[to: %s, subject: %s]", toList, subject);
  }

  private void populateRequestEmailProperties(Builder<String, Object> templateProperties, SingularityRequest request) {
    templateProperties.put("requestId", request.getId());
    templateProperties.put("singularityRequestLink", getSingularityRequestLink(request));

    templateProperties.put("requestScheduled", request.isScheduled());
    templateProperties.put("requestOneOff", request.isOneOff());

    templateProperties.put("taskWillRetry", request.getNumRetriesOnFailure().or(0) > 0);
    templateProperties.put("numRetries", request.getNumRetriesOnFailure().or(0));
  }

  private void populateTaskEmailProperties(Builder<String, Object> templateProperties, SingularityTaskId taskId, Collection<SingularityTaskHistoryUpdate> taskHistory, ExtendedTaskState taskState) {
    templateProperties.put("singularityTaskLink", getSingularityTaskLink(taskId));
    templateProperties.put("taskId", taskId.getId());
    templateProperties.put("deployId", taskId.getDeployId());

    templateProperties.put("taskDirectory", taskManager.getDirectory(taskId).or("directory missing"));

    Optional<SingularityTask> task = taskManager.getTask(taskId);

    if (task.isPresent()) {
      templateProperties.put("slaveHostname", task.get().getOffer().getHostname());
    }

    boolean needsBeenPrefix = (taskState == ExtendedTaskState.TASK_LOST) || (taskState == ExtendedTaskState.TASK_KILLED);

    templateProperties.put("status", String.format("%s%s", needsBeenPrefix ? "been " : "", taskState.getDisplayName()));
    templateProperties.put("taskStateLost", taskState == ExtendedTaskState.TASK_LOST);
    templateProperties.put("taskStateFailed", taskState == ExtendedTaskState.TASK_FAILED);
    templateProperties.put("taskStateFinished", taskState == ExtendedTaskState.TASK_FINISHED);
    templateProperties.put("taskStateKilled", taskState == ExtendedTaskState.TASK_KILLED);

    templateProperties.put("taskUpdates", JadeHelper.getJadeTaskHistory(taskHistory));
    templateProperties.put("taskRan", didTaskRun(taskHistory));
  }

  private Optional<TaskCleanupType> getTaskCleanupTypefromSingularityTaskHistoryUpdate(SingularityTaskHistoryUpdate taskHistoryUpdate) {
    if (!taskHistoryUpdate.getStatusMessage().isPresent()) {
      return Optional.absent();
    }

    try {
      return Optional.of(TaskCleanupType.valueOf(taskHistoryUpdate.getStatusMessage().get()));
    } catch (IllegalArgumentException iae) {
      LOG.warn("Couldn't parse TaskCleanupType from update {}", taskHistoryUpdate);
      return Optional.absent();
    }
  }

  private Collection<EmailDestination> getEmailDestination(ExtendedTaskState taskState, Collection<SingularityTaskHistoryUpdate> taskHistory) {
    Optional<EmailType> emailType = getEmailType(taskState, taskHistory);
    if (!emailType.isPresent()) {
      return Collections.emptyList();
    }
    return getDestination(emailType.get());
  }

  private Optional<EmailType> getEmailType(ExtendedTaskState taskState, Collection<SingularityTaskHistoryUpdate> taskHistory) {
    switch (taskState) {
      case TASK_FAILED:
        return Optional.of(EmailType.TASK_FAILED);
      case TASK_FINISHED:
        return Optional.of(EmailType.TASK_FINISHED);
      case TASK_KILLED:
        Optional<SingularityTaskHistoryUpdate> cleaningUpdate = SingularityTaskHistoryUpdate.getUpdate(taskHistory, ExtendedTaskState.TASK_CLEANING);

        if (cleaningUpdate.isPresent()) {
          Optional<TaskCleanupType> cleanupType = getTaskCleanupTypefromSingularityTaskHistoryUpdate(cleaningUpdate.get());

          if (cleanupType.isPresent()) {
            switch (cleanupType.get()) {
              case DECOMISSIONING:
                return Optional.of(EmailType.TASK_KILLED_DECOMISSIONED);
              case UNHEALTHY_NEW_TASK:
              case OVERDUE_NEW_TASK:
                return Optional.of(EmailType.TASK_KILLED_UNHEALTHY);
              default:
            }
          }
        }

        return Optional.of(EmailType.TASK_KILLED);
      case TASK_LOST:
        return Optional.of(EmailType.TASK_LOST);
      default:
        return Optional.absent();
    }
  }

  public void sendTaskCompletedMail(SingularityTaskId taskId, SingularityRequest request, ExtendedTaskState taskState) {
    if (!maybeSmtpConfiguration.isPresent()) {
      LOG.debug("Not sending task completed mail - no SMTP configuration is present");
      return;
    }

    final Collection<SingularityTaskHistoryUpdate> taskHistory = taskManager.getTaskHistoryUpdates(taskId);
    final Collection<EmailDestination> emailDestination = getEmailDestination(taskState, taskHistory);

    if (emailDestination.isEmpty()) {
      LOG.debug("Not configured to send task completed mail for {}", taskState);
      return;
    }

    final Builder<String, Object> templateProperties = ImmutableMap.<String, Object> builder();
    populateRequestEmailProperties(templateProperties, request);
    populateTaskEmailProperties(templateProperties, taskId, taskHistory, taskState);

    final String subject = getSubjectForTaskHistory(taskId, taskState, taskHistory);

    final String adminEmails = adminJoiner.join(maybeSmtpConfiguration.get().getAdmins());
    templateProperties.put("adminEmails", adminEmails);

    final String body = Jade4J.render(taskCompletedTemplate, templateProperties.build());

    queueMail(emailDestination, Optional.of(request), subject, body);
  }

  private List<EmailDestination> getDestination(EmailType type) {
    List<EmailDestination> fromMap = maybeSmtpConfiguration.get().getEmailConfiguration().get(type);
    if (fromMap == null) {
      return Collections.emptyList();
    }
    return fromMap;
  }

  public enum RequestMailType {

    PAUSED(EmailType.REQUEST_PAUSED), UNPAUSED(EmailType.REQUEST_UNPAUSED), REMOVED(EmailType.REQUEST_REMOVED);

    private final EmailType emailType;

    private RequestMailType(EmailType emailType) {
      this.emailType = emailType;
    }

    public EmailType getEmailType() {
      return emailType;
    }

  }

  private void sendRequestMail(SingularityRequest request, RequestMailType type, Optional<String> user) {
    if (!maybeSmtpConfiguration.isPresent()) {
      LOG.debug("Not sending request mail - no SMTP configuration is present");
      return;
    }

    final List<EmailDestination> emailDestination = getDestination(type.getEmailType());

    if (emailDestination.isEmpty()) {
      LOG.debug("Not configured to send request cooldown mail for");
      return;
    }

    final String subject = String.format("Request %s has been %s — Singularity", request.getId(), type.name().toLowerCase());
    final Builder<String, Object> templateProperties = ImmutableMap.<String, Object> builder();
    populateRequestEmailProperties(templateProperties, request);

    templateProperties.put("requestPaused", type == RequestMailType.PAUSED);
    templateProperties.put("requestUnpaused", type == RequestMailType.UNPAUSED);
    templateProperties.put("action", type.name().toLowerCase());
    templateProperties.put("hasUser", user.isPresent());

    if (user.isPresent()) {
      templateProperties.put("user", user.get());
    }

    final String body = Jade4J.render(requestModifiedTemplate, templateProperties.build());

    queueMail(emailDestination, Optional.of(request), subject, body);
  }

  public void sendRequestPausedMail(SingularityRequest request, Optional<String> user) {
    sendRequestMail(request, RequestMailType.PAUSED, user);
  }

  public void sendRequestUnpausedMail(SingularityRequest request, Optional<String> user) {
    sendRequestMail(request, RequestMailType.UNPAUSED, user);
  }

  public void sendRequestRemovedMail(SingularityRequest request, Optional<String> user) {
    sendRequestMail(request, RequestMailType.REMOVED, user);
  }

  public void sendRequestInCooldownMail(SingularityRequest request) {
    if (!maybeSmtpConfiguration.isPresent()) {
      LOG.debug("Not sending request in cooldown mail - no SMTP configuration is present");
      return;
    }

    final List<EmailDestination> emailDestination = getDestination(EmailType.REQUEST_IN_COOLDOWN);

    if (emailDestination.isEmpty()) {
      LOG.debug("Not configured to send request cooldown mail for");
      return;
    }

    final Builder<String, Object> templateProperties = ImmutableMap.<String, Object> builder();
    populateRequestEmailProperties(templateProperties, request);

    final String subject = String.format("Request %s has entered system cooldown — Singularity", request.getId());

    templateProperties.put("numFailures", configuration.getCooldownAfterFailures());
    templateProperties.put("cooldownDelayFormat", DurationFormatUtils.formatDurationHMS(TimeUnit.SECONDS.toMillis(configuration.getCooldownMinScheduleSeconds())));
    templateProperties.put("cooldownExpiresFormat", DurationFormatUtils.formatDurationHMS(TimeUnit.MINUTES.toMillis(configuration.getCooldownExpiresAfterMinutes())));

    final String body = Jade4J.render(requestInCooldownTemplate, templateProperties.build());

    queueMail(emailDestination, Optional.of(request), subject, body);
  }

  private boolean didTaskRun(Collection<SingularityTaskHistoryUpdate> history) {
    SimplifiedTaskState simplifiedTaskState = SingularityTaskHistoryUpdate.getCurrentState(history);

    return (simplifiedTaskState == SimplifiedTaskState.DONE) || (simplifiedTaskState == SimplifiedTaskState.RUNNING);
  }

  private String getSubjectForTaskHistory(SingularityTaskId taskId, ExtendedTaskState state, Collection<SingularityTaskHistoryUpdate> history) {
    if (!didTaskRun(history)) {
      return String.format("Task never started and was %s (%s)", state.getDisplayName(), taskId.toString());
    }

    return String.format("Task %s (%s)", state.getDisplayName(), taskId.toString());
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

  private void queueMail(final Collection<EmailDestination> destination, final Optional<SingularityRequest> request, final String subject, final String body) {
    final List<String> toList = Lists.newArrayList();
    final List<String> ccList = Lists.newArrayList();

    if (destination.contains(EmailDestination.OWNERS) && request.isPresent() && request.get().getOwners().isPresent()) {
      toList.addAll(request.get().getOwners().get());
      if (destination.contains(EmailDestination.ADMINS)) {
        ccList.addAll(maybeSmtpConfiguration.get().getAdmins());
      }
    } else if (destination.contains(EmailDestination.ADMINS)) {
      toList.addAll(maybeSmtpConfiguration.get().getAdmins());
    }

    if (toList.isEmpty()) {
      LOG.warn("Couldn't queue email {} because no to address is present", subject);
      return;
    }

    final Runnable cmd = new Runnable() {

      @Override
      public void run() {
        sendMail(toList, ccList, subject, body);
      }
    };

    LOG.debug("Queuing an email to {}/{} (subject: {})", toList, ccList, subject);

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

  private void sendMail(List<String> toList, List<String> ccList, String subject, String body) {
    final SMTPConfiguration smtpConfiguration = maybeSmtpConfiguration.get();

    boolean useAuth = false;

    if (smtpConfiguration.getUsername().isPresent() && smtpConfiguration.getPassword().isPresent()) {
      useAuth = true;
    } else if (smtpConfiguration.getUsername().isPresent() || smtpConfiguration.getPassword().isPresent()) {
      LOG.warn("Not using smtp authentication because username ({}) or password ({}) was not present", smtpConfiguration.getUsername().isPresent(), smtpConfiguration.getPassword().isPresent());
    }

    try {
      final Session session = createSession(maybeSmtpConfiguration.get(), useAuth);

      MimeMessage message = new MimeMessage(session);

      Address[] toArray = getAddresses(toList);
      message.addRecipients(RecipientType.TO, toArray);

      if (!ccList.isEmpty()) {
        Address[] ccArray = getAddresses(ccList);
        message.addRecipients(RecipientType.CC, ccArray);
      }

      message.setFrom(new InternetAddress(smtpConfiguration.getFrom()));

      message.setSubject(subject);
      message.setContent(body, "text/html; charset=utf-8");

      LOG.trace("Sending a message to {} - {}", Arrays.toString(toArray), getEmailLogFormat(toList, subject));

      Transport.send(message);
    } catch (Throwable t) {
      LOG.warn("Unable to send message {}", getEmailLogFormat(toList, subject), t);
      exceptionNotifier.notify(t);
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
