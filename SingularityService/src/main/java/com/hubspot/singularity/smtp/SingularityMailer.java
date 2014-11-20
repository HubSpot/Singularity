package com.hubspot.singularity.smtp;

import io.dropwizard.lifecycle.Managed;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.json.MesosFileChunkObject;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.SingularityMainModule;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup.TaskCleanupType;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskHistoryUpdate.SimplifiedTaskState;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.EmailConfigurationEnums.EmailDestination;
import com.hubspot.singularity.config.EmailConfigurationEnums.EmailType;
import com.hubspot.singularity.config.SMTPConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.MetadataManager;
import com.hubspot.singularity.data.SandboxManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

import de.neuland.jade4j.Jade4J;
import de.neuland.jade4j.template.JadeTemplate;

public class SingularityMailer implements Managed {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityMailer.class);

  private final SingularitySmtpSender smtpSender;
  private final SingularityConfiguration configuration;
  private final Optional<SMTPConfiguration> maybeSmtpConfiguration;
  private final Optional<ThreadPoolExecutor> mailPreparerExecutorService;
  private final SingularityExceptionNotifier exceptionNotifier;

  private final TaskManager taskManager;

  private final JadeTemplate taskCompletedTemplate;
  private final JadeTemplate requestInCooldownTemplate;
  private final JadeTemplate requestModifiedTemplate;
  private final JadeTemplate rateLimitedTemplate;

  private final MetadataManager metadataManager;
  private final SandboxManager sandboxManager;

  private final Optional<String> uiHostnameAndPath;

  private final Joiner adminJoiner;

  private static final String TASK_LINK_FORMAT = "%s/task/%s";
  private static final String REQUEST_LINK_FORMAT = "%s/request/%s";

  @Inject
  public SingularityMailer(SingularitySmtpSender smtpSender, SingularityConfiguration configuration,
      TaskManager taskManager,
      SandboxManager sandboxManager,
      MetadataManager metadataManager,
      SingularityExceptionNotifier exceptionNotifier,
      @Named(SingularityMainModule.TASK_COMPLETED_TEMPLATE) JadeTemplate taskCompletedTemplate,
      @Named(SingularityMainModule.REQUEST_IN_COOLDOWN_TEMPLATE) JadeTemplate requestInCooldownTemplate,
      @Named(SingularityMainModule.REQUEST_MODIFIED_TEMPLATE) JadeTemplate requestModifiedTemplate,
      @Named(SingularityMainModule.RATE_LIMITED_TEMPLATE) JadeTemplate rateLimitedTemplate) {

    this.smtpSender = smtpSender;
    this.maybeSmtpConfiguration = configuration.getSmtpConfiguration();
    this.configuration = configuration;
    this.uiHostnameAndPath = configuration.getUiConfiguration().getBaseUrl();
    this.taskManager = taskManager;
    this.sandboxManager = sandboxManager;
    this.metadataManager = metadataManager;
    this.exceptionNotifier = exceptionNotifier;
    this.adminJoiner = Joiner.on(", ").skipNulls();

    this.requestModifiedTemplate = requestModifiedTemplate;
    this.taskCompletedTemplate = taskCompletedTemplate;
    this.requestInCooldownTemplate = requestInCooldownTemplate;
    this.rateLimitedTemplate = rateLimitedTemplate;

    if (maybeSmtpConfiguration.isPresent()) {
      this.mailPreparerExecutorService = Optional.of(JavaUtils.newFixedTimingOutThreadPool(maybeSmtpConfiguration.get().getMailMaxThreads(), TimeUnit.SECONDS.toMillis(1), "SingularityMailPreparer-%d"));;
    } else {
      this.mailPreparerExecutorService = Optional.absent();
    }
  }

  @Override
  public void start() throws Exception {
  }

  @Override
  public void stop() throws Exception {
    if (mailPreparerExecutorService.isPresent()) {
      MoreExecutors.shutdownAndAwaitTermination(mailPreparerExecutorService.get(), 1, TimeUnit.SECONDS);
    }
  }

  private Optional<String[]> getTaskLogFile(final SingularityTaskId taskId, final String filename, final Optional<SingularityTask> task, final Optional<String> directory) {
    if (!task.isPresent() || !directory.isPresent()) {
      LOG.warn("Couldn't retrieve {} for {} because task ({}) or directory ({}) wasn't present", filename, taskId, task.isPresent(), directory.isPresent());
      return Optional.absent();
    }

    final String slaveHostname = task.get().getOffer().getHostname();

    final String fullPath = String.format("%s/%s", directory.get(), filename);

    final Long logLength = Long.valueOf(maybeSmtpConfiguration.get().getTaskLogLength());

    final Optional<MesosFileChunkObject> logChunkObject;

    try {
      logChunkObject = sandboxManager.read(slaveHostname, fullPath, Optional.of(0L), Optional.of(logLength));
    } catch (RuntimeException e) {
      LOG.error("Sandboxmanager failed to read {}/{} on slave {}", directory.get(), filename, slaveHostname, e);
      return Optional.absent();
    }

    if (logChunkObject.isPresent()) {
      return Optional.of(logChunkObject.get().getData().split("[\r\n]+"));
    } else {
      LOG.error("Singularity mailer failed to get {} log for {} task ", filename, taskId.getId());
      return Optional.absent();
    }
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
    Optional<SingularityTask> task = taskManager.getTask(taskId);
    Optional<String> directory = taskManager.getDirectory(taskId);

    templateProperties.put("singularityTaskLink", getSingularityTaskLink(taskId));
    templateProperties.put("stdout", getTaskLogFile(taskId, "stdout", task, directory).or(new String[0]));
    templateProperties.put("stderr", getTaskLogFile(taskId, "stderr", task, directory).or(new String[0]));
    templateProperties.put("taskId", taskId.getId());
    templateProperties.put("deployId", taskId.getDeployId());

    templateProperties.put("taskDirectory", directory.or("directory missing"));

    if (task.isPresent()) {
      templateProperties.put("slaveHostname", task.get().getOffer().getHostname());
    }

    boolean needsBeenPrefix = taskState == ExtendedTaskState.TASK_LOST || taskState == ExtendedTaskState.TASK_KILLED;

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

  private Optional<EmailType> getEmailType(ExtendedTaskState taskState, SingularityRequest request, Collection<SingularityTaskHistoryUpdate> taskHistory) {
    switch (taskState) {
      case TASK_FAILED:
        return Optional.of(EmailType.TASK_FAILED);
      case TASK_FINISHED:
        if (request.isScheduled()) {
          return Optional.of(EmailType.TASK_FINISHED);
        }
        return Optional.of(EmailType.TASK_FINISHED_NON_SCHEDULED_REQUEST);
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

  public void sendTaskCompletedMail(final SingularityTaskId taskId, final SingularityRequest request, final ExtendedTaskState taskState) {
    if (!maybeSmtpConfiguration.isPresent()) {
      LOG.debug("Not sending task completed mail - no SMTP configuration is present");
      return;
    }

    mailPreparerExecutorService.get().submit(new Runnable() {

      @Override
      public void run() {
        try {
          prepareTaskCompletedMail(taskId, request, taskState);
        } catch (Throwable t) {
          LOG.error("While preparing task completed mail for {}", taskId, t);
          exceptionNotifier.notify(t);
        }
      }
    });
  }

  private void prepareTaskCompletedMail(SingularityTaskId taskId, SingularityRequest request, ExtendedTaskState taskState) {
    final Collection<SingularityTaskHistoryUpdate> taskHistory = taskManager.getTaskHistoryUpdates(taskId);
    final Optional<EmailType> emailType = getEmailType(taskState, request, taskHistory);

    if (!emailType.isPresent()) {
      LOG.debug("No configured emailType for {} and {}", request, taskState);
      return;
    }

    final Collection<EmailDestination> emailDestination = getDestination(emailType.get());

    if (emailDestination.isEmpty()) {
      LOG.debug("Not configured to send task completed mail for {}", taskState);
      return;
    }

    final Builder<String, Object> templateProperties = ImmutableMap.<String, Object>builder();
    populateRequestEmailProperties(templateProperties, request);
    populateTaskEmailProperties(templateProperties, taskId, taskHistory, taskState);

    final String subject = getSubjectForTaskHistory(taskId, taskState, taskHistory);

    final String adminEmails = adminJoiner.join(maybeSmtpConfiguration.get().getAdmins());
    templateProperties.put("adminEmails", adminEmails);

    final String body = Jade4J.render(taskCompletedTemplate, templateProperties.build());

    queueMail(emailDestination, request, emailType.get(), subject, body);
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

  private void sendRequestMail(final SingularityRequest request, final RequestMailType type, final Optional<String> user) {
    if (!maybeSmtpConfiguration.isPresent()) {
      LOG.debug("Not sending request mail - no SMTP configuration is present");
      return;
    }

    mailPreparerExecutorService.get().submit(new Runnable() {

      @Override
      public void run() {
        try {
          prepareRequestMail(request, type, user);
        } catch (Throwable t) {
          LOG.error("While preparing request mail for {} / {}", request, type, t);
          exceptionNotifier.notify(t);
        }
      }
    });
  }

  private void prepareRequestMail(SingularityRequest request, RequestMailType type, Optional<String> user) {
    final List<EmailDestination> emailDestination = getDestination(type.getEmailType());

    if (emailDestination.isEmpty()) {
      LOG.debug("Not configured to send request cooldown mail for");
      return;
    }

    final String subject = String.format("Request %s has been %s — Singularity", request.getId(), type.name().toLowerCase());
    final Builder<String, Object> templateProperties = ImmutableMap.<String, Object>builder();
    populateRequestEmailProperties(templateProperties, request);

    templateProperties.put("requestPaused", type == RequestMailType.PAUSED);
    templateProperties.put("requestUnpaused", type == RequestMailType.UNPAUSED);
    templateProperties.put("action", type.name().toLowerCase());
    templateProperties.put("hasUser", user.isPresent());

    if (user.isPresent()) {
      templateProperties.put("user", user.get());
    }

    final String body = Jade4J.render(requestModifiedTemplate, templateProperties.build());

    queueMail(emailDestination, request, type.getEmailType(), subject, body);
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

  public void sendRequestInCooldownMail(final SingularityRequest request) {
    if (!maybeSmtpConfiguration.isPresent()) {
      LOG.debug("Not sending request in cooldown mail - no SMTP configuration is present");
      return;
    }

    mailPreparerExecutorService.get().submit(new Runnable() {

      @Override
      public void run() {
        try {
          prepareRequestInCooldownMail(request);
        } catch (Throwable t) {
          LOG.error("While preparing request in cooldown mail for {}", request, t);
          exceptionNotifier.notify(t);
        }
      }
    });
  }

  private void prepareRequestInCooldownMail(SingularityRequest request) {
    final List<EmailDestination> emailDestination = getDestination(EmailType.REQUEST_IN_COOLDOWN);

    if (emailDestination.isEmpty()) {
      LOG.debug("Not configured to send request cooldown mail for");
      return;
    }

    final Builder<String, Object> templateProperties = ImmutableMap.<String, Object>builder();
    populateRequestEmailProperties(templateProperties, request);

    final String subject = String.format("Request %s has entered system cooldown — Singularity", request.getId());

    templateProperties.put("numFailures", configuration.getCooldownAfterFailures());
    templateProperties.put("cooldownDelayFormat", DurationFormatUtils.formatDurationHMS(TimeUnit.SECONDS.toMillis(configuration.getCooldownMinScheduleSeconds())));
    templateProperties.put("cooldownExpiresFormat", DurationFormatUtils.formatDurationHMS(TimeUnit.MINUTES.toMillis(configuration.getCooldownExpiresAfterMinutes())));

    final String body = Jade4J.render(requestInCooldownTemplate, templateProperties.build());

    queueMail(emailDestination, request, EmailType.REQUEST_IN_COOLDOWN, subject, body);
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

  private enum RateLimitResult {
    SEND_MAIL, DONT_SEND_MAIL_IN_COOLDOWN, SEND_COOLDOWN_STARTED_MAIL;
  }

  private RateLimitResult checkRateLimitForMail(SingularityRequest request, EmailType emailType) {
    if (maybeSmtpConfiguration.get().getRateLimitAfterNotifications() < 1) {
      LOG.trace("Mail rate limit cooldown disabled");
      return RateLimitResult.SEND_MAIL;
    }

    final String requestId = request.getId();
    final String emailTypeName = emailType.name();

    final long now = System.currentTimeMillis();

    final Optional<String> cooldownMarker = metadataManager.getMailCooldownMarker(requestId, emailTypeName);

    if (cooldownMarker.isPresent()) {
      final long cooldownLeft = maybeSmtpConfiguration.get().getRateLimitCooldownMillis() - (now - Long.parseLong(cooldownMarker.get()));

      if (cooldownLeft > 0) {
        LOG.debug("Not sending {} for {} - mail cooldown has {} time left out of {}", emailTypeName, requestId, cooldownLeft, maybeSmtpConfiguration.get().getRateLimitCooldownMillis());
        return RateLimitResult.DONT_SEND_MAIL_IN_COOLDOWN;
      }

      metadataManager.removeMailCooldown(requestId, emailTypeName);
    }

    metadataManager.saveMailRecord(request, emailType);

    int numInPeriod = 0;

    for (String recentMailRecord : metadataManager.getMailRecords(request.getId(), emailType.name())) {
      if (now - Long.parseLong(recentMailRecord) < maybeSmtpConfiguration.get().getRateLimitPeriodMillis()) {
        numInPeriod++;
      }
    }

    if (numInPeriod > maybeSmtpConfiguration.get().getRateLimitAfterNotifications()) {
      LOG.info("{} for {} sent at least {} times in {}, not sending this mail again for at least {}", emailTypeName, requestId, numInPeriod, maybeSmtpConfiguration.get().getRateLimitAfterNotifications(), maybeSmtpConfiguration.get().getRateLimitCooldownMillis());
      metadataManager.cooldownMail(requestId, emailTypeName);
      return RateLimitResult.SEND_COOLDOWN_STARTED_MAIL;
    }

    return RateLimitResult.SEND_MAIL;
  }

  private Map<String, Object> getRateLimitTemplateProperties(SingularityRequest request, final EmailType emailType) {
    final Builder<String, Object> templateProperties = ImmutableMap.<String, Object>builder();

    templateProperties.put("singularityRequestLink", getSingularityRequestLink(request));
    templateProperties.put("rateLimitAfterNotifications", Integer.toString(maybeSmtpConfiguration.get().getRateLimitAfterNotifications()));
    templateProperties.put("rateLimitPeriodFormat", DurationFormatUtils.formatDurationHMS(maybeSmtpConfiguration.get().getRateLimitPeriodMillis()));
    templateProperties.put("rateLimitCooldownFormat", DurationFormatUtils.formatDurationHMS(maybeSmtpConfiguration.get().getRateLimitCooldownMillis()));
    templateProperties.put("emailType", emailType.name());
    templateProperties.put("requestId", request.getId());

    return templateProperties.build();
  }

  private void queueMail(final Collection<EmailDestination> destination, final SingularityRequest request, final EmailType emailType, String subject, String body) {
    RateLimitResult result = checkRateLimitForMail(request, emailType);

    if (result == RateLimitResult.DONT_SEND_MAIL_IN_COOLDOWN) {
      return;
    }

    if (result == RateLimitResult.SEND_COOLDOWN_STARTED_MAIL) {
      subject = String.format("%s notifications for %s are being rate limited", emailType.name(), request.getId());
      body = Jade4J.render(rateLimitedTemplate, getRateLimitTemplateProperties(request, emailType));
    }

    final List<String> toList = Lists.newArrayList();
    final List<String> ccList = Lists.newArrayList();

    if (destination.contains(EmailDestination.OWNERS) && request.getOwners().isPresent() && !request.getOwners().get().isEmpty()) {
      toList.addAll(request.getOwners().get());
      if (destination.contains(EmailDestination.ADMINS)) {
        ccList.addAll(maybeSmtpConfiguration.get().getAdmins());
      }
    } else if (destination.contains(EmailDestination.ADMINS)) {
      toList.addAll(maybeSmtpConfiguration.get().getAdmins());
    }

    smtpSender.queueMail(toList, ccList, subject, body);
  }

}
