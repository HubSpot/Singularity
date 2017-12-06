package com.hubspot.singularity.smtp;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityDisastersData;
import com.hubspot.singularity.SingularityEmailDestination;
import com.hubspot.singularity.SingularityEmailType;
import com.hubspot.singularity.SingularityMainModule;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskMetadata;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.api.SingularityPauseRequest;
import com.hubspot.singularity.api.SingularityScaleRequest;
import com.hubspot.singularity.config.SMTPConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DisasterManager;
import com.hubspot.singularity.data.MetadataManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

import de.neuland.jade4j.Jade4J;
import de.neuland.jade4j.template.JadeTemplate;
import io.dropwizard.lifecycle.Managed;

public class SmtpMailer implements SingularityMailer, Managed {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityMailer.class);

  private final SingularitySmtpSender smtpSender;
  private final SingularityConfiguration configuration;
  private final SMTPConfiguration smtpConfiguration;
  private final ThreadPoolExecutor mailPreparerExecutorService;
  private final SingularityExceptionNotifier exceptionNotifier;

  private final TaskManager taskManager;

  private final JadeTemplate taskTemplate;
  private final JadeTemplate requestInCooldownTemplate;
  private final JadeTemplate requestModifiedTemplate;
  private final JadeTemplate rateLimitedTemplate;
  private final JadeTemplate disastersTemplate;

  private final MetadataManager metadataManager;

  private final Joiner adminJoiner;
  private final MailTemplateHelpers mailTemplateHelpers;

  private final DisasterManager disasterManager;

  private static final Pattern TASK_STATUS_BY_PATTERN = Pattern.compile("(\\w+) by \\w+");

  @Inject
  public SmtpMailer(
      SingularitySmtpSender smtpSender,
      SingularityConfiguration configuration,
      TaskManager taskManager,
      MetadataManager metadataManager,
      SingularityExceptionNotifier exceptionNotifier,
      MailTemplateHelpers mailTemplateHelpers,
      DisasterManager disasterManager,
      @Named(SingularityMainModule.TASK_TEMPLATE) JadeTemplate taskTemplate,
      @Named(SingularityMainModule.REQUEST_IN_COOLDOWN_TEMPLATE) JadeTemplate requestInCooldownTemplate,
      @Named(SingularityMainModule.REQUEST_MODIFIED_TEMPLATE) JadeTemplate requestModifiedTemplate,
      @Named(SingularityMainModule.RATE_LIMITED_TEMPLATE) JadeTemplate rateLimitedTemplate,
      @Named(SingularityMainModule.DISASTERS_TEMPLATE) JadeTemplate disastersTemplate) {

    this.smtpSender = smtpSender;
    this.smtpConfiguration = configuration.getSmtpConfigurationOptional().get();
    this.configuration = configuration;
    this.taskManager = taskManager;
    this.metadataManager = metadataManager;
    this.exceptionNotifier = exceptionNotifier;
    this.adminJoiner = Joiner.on(", ").skipNulls();

    this.mailTemplateHelpers = mailTemplateHelpers;

    this.requestModifiedTemplate = requestModifiedTemplate;
    this.taskTemplate = taskTemplate;
    this.requestInCooldownTemplate = requestInCooldownTemplate;
    this.rateLimitedTemplate = rateLimitedTemplate;
    this.disastersTemplate = disastersTemplate;
    this.disasterManager = disasterManager;

    this.mailPreparerExecutorService = JavaUtils.newFixedTimingOutThreadPool(smtpConfiguration.getMailMaxThreads(), TimeUnit.SECONDS.toMillis(1), "SingularityMailPreparer-%d");
  }

  @Override
  public void start() throws Exception {
  }

  @Override
  public void stop() throws Exception {
    MoreExecutors.shutdownAndAwaitTermination(mailPreparerExecutorService, 1, TimeUnit.SECONDS);
  }

  private void populateRequestEmailProperties(Map<String, Object> templateProperties, SingularityRequest request, SingularityEmailType emailType) {
    templateProperties.put("requestId", request.getId());
    templateProperties.put("singularityRequestLink", mailTemplateHelpers.getSingularityRequestLink(request.getId()));

    templateProperties.put("requestAlwaysRunning", request.isAlwaysRunning());
    templateProperties.put("requestRunOnce", request.getRequestType() == RequestType.RUN_ONCE);
    templateProperties.put("requestScheduled", request.isScheduled());
    templateProperties.put("requestOneOff", request.isOneOff());

    templateProperties.put("taskWillRetry", request.getNumRetriesOnFailure().or(0) > 0);
    templateProperties.put("numRetries", request.getNumRetriesOnFailure().or(0));

    templateProperties.put("color", emailType.getColor());
  }

  private void populateTaskEmailProperties(Map<String, Object> templateProperties, SingularityTaskId taskId, Collection<SingularityTaskHistoryUpdate> taskHistory, ExtendedTaskState taskState, List<SingularityTaskMetadata> taskMetadata, SingularityEmailType emailType) {
    Optional<SingularityTask> task = taskManager.getTask(taskId);
    Optional<String> directory = taskManager.getDirectory(taskId);

    templateProperties.put("singularityTaskLink", mailTemplateHelpers.getSingularityTaskLink(taskId.getId()));

    // Grab the tails of log files from remote mesos slaves.
    templateProperties.put("logTails", mailTemplateHelpers.getTaskLogs(taskId, task, directory));

    templateProperties.put("taskId", taskId.getId());
    templateProperties.put("deployId", taskId.getDeployId());

    templateProperties.put("taskDirectory", directory.or("directory missing"));

    templateProperties.put("color", emailType.getColor());

    if (task.isPresent()) {
      templateProperties.put("slaveHostname", task.get().getHostname());
      if (task.get().getTaskRequest().getPendingTask().getCmdLineArgsList().isPresent()) {
        templateProperties.put("extraCmdLineArguments", task.get().getTaskRequest().getPendingTask().getCmdLineArgsList().get());
      }
    }

    boolean needsBeenPrefix = taskState == ExtendedTaskState.TASK_LOST || taskState == ExtendedTaskState.TASK_KILLED;

    templateProperties.put("status", String.format("%s%s", needsBeenPrefix ? "has been " : "has ", taskState.getDisplayName()));
    templateProperties.put("taskStateLost", taskState == ExtendedTaskState.TASK_LOST);
    templateProperties.put("taskStateFailed", taskState == ExtendedTaskState.TASK_FAILED);
    templateProperties.put("taskStateFinished", taskState == ExtendedTaskState.TASK_FINISHED);
    templateProperties.put("taskStateKilled", taskState == ExtendedTaskState.TASK_KILLED);
    templateProperties.put("taskStateRunning", taskState == ExtendedTaskState.TASK_RUNNING);

    templateProperties.put("taskHasMetadata", !taskMetadata.isEmpty());
    templateProperties.put("taskMetadata", mailTemplateHelpers.getJadeTaskMetadata(taskMetadata));
    templateProperties.put("taskUpdates", mailTemplateHelpers.getJadeTaskHistory(taskHistory));
    templateProperties.put("taskRan", mailTemplateHelpers.didTaskRun(taskHistory));
  }

  private static Optional<TaskCleanupType> getTaskCleanupTypefromSingularityTaskHistoryUpdate(SingularityTaskHistoryUpdate taskHistoryUpdate) {
    if (!taskHistoryUpdate.getStatusMessage().isPresent()) {
      return Optional.absent();
    }

    String taskCleanupTypeMsg = taskHistoryUpdate.getStatusMessage().get();

    Matcher matcher = TASK_STATUS_BY_PATTERN.matcher(taskCleanupTypeMsg);

    if (matcher.find()) {
      taskCleanupTypeMsg = matcher.group(1);
    }

    try {
      return Optional.of(TaskCleanupType.valueOf(taskCleanupTypeMsg.toUpperCase()));
    } catch (IllegalArgumentException iae) {
      LOG.warn("Couldn't parse TaskCleanupType from update {}", taskHistoryUpdate);
      return Optional.absent();
    }
  }

  private Optional<SingularityEmailType> getEmailType(ExtendedTaskState taskState, SingularityRequest request, Collection<SingularityTaskHistoryUpdate> taskHistory) {
    final Optional<SingularityTaskHistoryUpdate> cleaningUpdate = SingularityTaskHistoryUpdate.getUpdate(taskHistory, ExtendedTaskState.TASK_CLEANING);

    switch (taskState) {
      case TASK_FAILED:
        if (cleaningUpdate.isPresent()) {
          Optional<TaskCleanupType> cleanupType = getTaskCleanupTypefromSingularityTaskHistoryUpdate(cleaningUpdate.get());

          if (cleanupType.isPresent() && cleanupType.get() == TaskCleanupType.DECOMISSIONING) {
            return Optional.of(SingularityEmailType.TASK_FAILED_DECOMISSIONED);
          }
        }
        return Optional.of(SingularityEmailType.TASK_FAILED);
      case TASK_FINISHED:
        switch (request.getRequestType()) {
          case ON_DEMAND:
            return Optional.of(SingularityEmailType.TASK_FINISHED_ON_DEMAND);
          case RUN_ONCE:
            return Optional.of(SingularityEmailType.TASK_FINISHED_RUN_ONCE);
          case SCHEDULED:
            return Optional.of(SingularityEmailType.TASK_FINISHED_SCHEDULED);
          case SERVICE:
          case WORKER:
            return Optional.of(SingularityEmailType.TASK_FINISHED_LONG_RUNNING);
        }
      case TASK_KILLED:
        if (cleaningUpdate.isPresent()) {
          Optional<TaskCleanupType> cleanupType = getTaskCleanupTypefromSingularityTaskHistoryUpdate(cleaningUpdate.get());

          if (cleanupType.isPresent()) {
            switch (cleanupType.get()) {
              case DECOMISSIONING:
                return Optional.of(SingularityEmailType.TASK_KILLED_DECOMISSIONED);
              case UNHEALTHY_NEW_TASK:
              case OVERDUE_NEW_TASK:
                return Optional.of(SingularityEmailType.TASK_KILLED_UNHEALTHY);
              default:
            }
          }
        }

        return Optional.of(SingularityEmailType.TASK_KILLED);
      case TASK_LOST:
        return Optional.of(SingularityEmailType.TASK_LOST);
      default:
        return Optional.absent();
    }
  }

  @Override
  public void sendTaskOverdueMail(final Optional<SingularityTask> task, final SingularityTaskId taskId, final SingularityRequest request, final long runTime, final long expectedRuntime) {
    final Builder<String, Object> templateProperties = ImmutableMap.<String, Object>builder();

    templateProperties.put("runTime", DurationFormatUtils.formatDurationHMS(runTime));
    templateProperties.put("expectedRunTime", DurationFormatUtils.formatDurationHMS(expectedRuntime));
    templateProperties.put("warningThreshold", String.format("%s%%", configuration.getWarnIfScheduledJobIsRunningPastNextRunPct()));

    templateProperties.put("status", "is overdue to finish");

    prepareTaskMail(task, taskId, request, SingularityEmailType.TASK_SCHEDULED_OVERDUE_TO_FINISH, templateProperties.build(), taskManager.getTaskHistoryUpdates(taskId),
        ExtendedTaskState.TASK_RUNNING, Collections.<SingularityTaskMetadata> emptyList());
  }

  @Override
  public void queueTaskCompletedMail(final Optional<SingularityTask> task, final SingularityTaskId taskId, final SingularityRequest request, final ExtendedTaskState taskState) {
    if (shouldQueueMail(task, taskId, request, taskState)) {
      taskManager.saveTaskFinishedInMailQueue(taskId);
    }
  }

  private boolean shouldQueueMail(final Optional<SingularityTask> task, final SingularityTaskId taskId, final SingularityRequest request, final ExtendedTaskState taskState) {
    final Collection<SingularityTaskHistoryUpdate> taskHistory = taskManager.getTaskHistoryUpdates(taskId);
    final Optional<SingularityEmailType> emailType = getEmailType(taskState, request, taskHistory);

    if (!emailType.isPresent()) {
      LOG.debug("No configured emailType for {} and {}", request, taskState);
      return false;
    }

    final Collection<SingularityEmailDestination> emailDestination = getDestination(request, emailType.get());

    if (emailDestination.isEmpty()) {
      LOG.debug("Not configured to send task mail for {}", emailType);
      return false;
    }

    if (disasterManager.isDisabled(SingularityAction.SEND_EMAIL)) {
      LOG.debug("Not sending email because SEND_EMAIL action is disabled.");
      return false;
    }

    RateLimitStatus rateLimitStatus = getCurrentRateLimitForMail(request, emailType.get());

    switch (rateLimitStatus) {
      case RATE_LIMITED:
        return false;
      case RATE_LIMITING_DISABLED:
      case NOT_RATE_LIMITED:
      default:
        return true;
    }
  }

  private void prepareTaskMail(Optional<SingularityTask> task, SingularityTaskId taskId, SingularityRequest request, SingularityEmailType emailType, Map<String, Object> extraProperties,
                               Collection<SingularityTaskHistoryUpdate> taskHistory, ExtendedTaskState taskState, List<SingularityTaskMetadata> taskMetadata) {
    final Collection<SingularityEmailDestination> emailDestination = getDestination(request, emailType);

    final Map<String, Object> templateProperties = Maps.newHashMap();
    populateRequestEmailProperties(templateProperties, request, emailType);
    populateTaskEmailProperties(templateProperties, taskId, taskHistory, taskState, taskMetadata, emailType);
    templateProperties.putAll(extraProperties);

    final String subject = mailTemplateHelpers.getSubjectForTaskHistory(taskId, taskState, emailType, taskHistory);

    final String adminEmails = adminJoiner.join(smtpConfiguration.getAdmins());
    templateProperties.put("adminEmails", adminEmails);

    final String body = Jade4J.render(taskTemplate, templateProperties);

    final Optional<String> user = task.isPresent() ? task.get().getTaskRequest().getPendingTask().getUser() : Optional.<String> absent();

    queueMail(emailDestination, request, emailType, user, subject, body);
  }

  @Override
  public void sendTaskCompletedMail(SingularityTaskHistory taskHistory, SingularityRequest request) {
    final Optional<SingularityTaskHistoryUpdate> lastUpdate = taskHistory.getLastTaskUpdate();

    if (!lastUpdate.isPresent()) {
      LOG.warn("Can't send task completed mail for task {} - no last update", taskHistory.getTask().getTaskId());
      return;
    }

    final Optional<SingularityEmailType> emailType = getEmailType(lastUpdate.get().getTaskState(), request, taskHistory.getTaskUpdates());

    if (!emailType.isPresent()) {
      LOG.debug("No configured emailType for {} and {}", request, lastUpdate.get().getTaskState());
      return;
    }

    prepareTaskMail(Optional.of(taskHistory.getTask()), taskHistory.getTask().getTaskId(), request, emailType.get(), Collections.<String, Object> emptyMap(),
        taskHistory.getTaskUpdates(), lastUpdate.get().getTaskState(), taskHistory.getTaskMetadata());
  }

  private List<SingularityEmailDestination> getDestination(SingularityRequest request, SingularityEmailType type) {
    // check for request-level email override
    if (request.getEmailConfigurationOverrides().isPresent() && request.getEmailConfigurationOverrides().get().get(type) != null) {
      return request.getEmailConfigurationOverrides().get().get(type);
    }

    List<SingularityEmailDestination> fromMap = smtpConfiguration.getEmailConfiguration().get(type);
    if (fromMap == null) {
      return Collections.emptyList();
    }
    return fromMap;
  }

  private enum RequestMailType {

    PAUSED(SingularityEmailType.REQUEST_PAUSED), UNPAUSED(SingularityEmailType.REQUEST_UNPAUSED), REMOVED(SingularityEmailType.REQUEST_REMOVED), SCALED(SingularityEmailType.REQUEST_SCALED);

    private final SingularityEmailType emailType;

    private RequestMailType(SingularityEmailType emailType) {
      this.emailType = emailType;
    }

    public SingularityEmailType getEmailType() {
      return emailType;
    }

  }

  private void sendRequestMail(final SingularityRequest request, final RequestMailType type, final Optional<String> user, final Optional<String> message, final Optional<Map<String, Object>> additionalProperties) {
    mailPreparerExecutorService.submit(new Runnable() {

      @Override
      public void run() {
        try {
          prepareRequestMail(request, type, user, message, additionalProperties);
        } catch (Throwable t) {
          LOG.error("While preparing request mail for {} / {}", request, type, t);
          exceptionNotifier.notify(String.format("Error preparing request mail (%s)", t.getMessage()), t, ImmutableMap.of("requestId", request.getId()));
        }
      }
    });
  }

  private void prepareRequestMail(SingularityRequest request, RequestMailType type, Optional<String> user, Optional<String> message, Optional<Map<String, Object>> additionalProperties) {
    final List<SingularityEmailDestination> emailDestination = getDestination(request, type.getEmailType());

    if (emailDestination.isEmpty()) {
      LOG.debug("Not configured to send request mail for {}", request);
      return;
    }

    final String subject = String.format("Request %s has been %s — Singularity", request.getId(), type.name().toLowerCase());
    final Map<String, Object> templateProperties = Maps.newHashMap();
    populateRequestEmailProperties(templateProperties, request, type.getEmailType());

    templateProperties.put("expiring", Boolean.FALSE);
    templateProperties.put("requestPaused", type == RequestMailType.PAUSED);
    templateProperties.put("requestUnpaused", type == RequestMailType.UNPAUSED);
    templateProperties.put("requestScaled", type == RequestMailType.SCALED);
    templateProperties.put("action", type.name().toLowerCase());
    templateProperties.put("hasUser", user.isPresent());
    templateProperties.put("hasMessage", message.isPresent());

    if (user.isPresent()) {
      templateProperties.put("user", user.get());
    }

    if (message.isPresent()) {
      templateProperties.put("message", message.get());
    }

    if (additionalProperties.isPresent()) {
      templateProperties.putAll(additionalProperties.get());
    }

    final String body = Jade4J.render(requestModifiedTemplate, templateProperties);

    queueMail(emailDestination, request, type.getEmailType(), user, subject, body);
  }

  private void setupExpireFormat(Map<String, Object> additionalProperties, Optional<Long> durationMillis) {
    if (!durationMillis.isPresent()) {
      return;
    }

    additionalProperties.put("expiring", Boolean.TRUE);

    final long now = System.currentTimeMillis();
    final long future = now + durationMillis.get();
    additionalProperties.put("expireFormat", DateFormatUtils.format(new Date(future), smtpConfiguration.getMailerDatePattern(), smtpConfiguration.getMailerTimeZone()));
  }

  @Override
  public void sendRequestPausedMail(SingularityRequest request, Optional<SingularityPauseRequest> pauseRequest, Optional<String> user) {
    Map<String, Object> additionalProperties = new HashMap<>();

    Boolean killTasks = Boolean.TRUE;

    Optional<String> message = Optional.absent();

    if (pauseRequest.isPresent()) {
      setupExpireFormat(additionalProperties, pauseRequest.get().getDurationMillis());

      if (pauseRequest.get().getKillTasks().isPresent()) {
        killTasks = pauseRequest.get().getKillTasks().get();
      }

      message = pauseRequest.get().getMessage();
    }

    additionalProperties.put("killTasks", killTasks);

    sendRequestMail(request, RequestMailType.PAUSED, user, message, Optional.of(additionalProperties));
  }

  @Override
  public void sendRequestUnpausedMail(SingularityRequest request, Optional<String> user, Optional<String> message) {
    sendRequestMail(request, RequestMailType.UNPAUSED, user, message, Optional.<Map<String, Object>> absent());
  }

  @Override
  public void sendRequestScaledMail(SingularityRequest request, Optional<SingularityScaleRequest> newScaleRequest, Optional<Integer> formerInstances, Optional<String> user) {
    Map<String, Object> additionalProperties = new HashMap<>();

    Optional<String> message = Optional.absent();

    if (newScaleRequest.isPresent()) {
      setupExpireFormat(additionalProperties, newScaleRequest.get().getDurationMillis());
      message = newScaleRequest.get().getMessage();
    }

    additionalProperties.put("newInstances", request.getInstancesSafe());
    additionalProperties.put("oldInstances", formerInstances.or(1));

    sendRequestMail(request, RequestMailType.SCALED, user, message, Optional.of(additionalProperties));
  }

  @Override
  public void sendRequestRemovedMail(SingularityRequest request, Optional<String> user, Optional<String> message) {
    sendRequestMail(request, RequestMailType.REMOVED, user, message, Optional.<Map<String, Object>> absent());
  }

  @Override
  public void sendRequestInCooldownMail(final SingularityRequest request) {
    mailPreparerExecutorService.submit(new Runnable() {

      @Override
      public void run() {
        try {
          prepareRequestInCooldownMail(request);
        } catch (Throwable t) {
          LOG.error("While preparing request in cooldown mail for {}", request, t);
          exceptionNotifier.notify(String.format("Error preparing cooldown mail (%s)", t.getMessage()), t, ImmutableMap.of("requestId", request.getId()));
        }
      }
    });
  }

  private void prepareRequestInCooldownMail(SingularityRequest request) {
    final List<SingularityEmailDestination> emailDestination = getDestination(request, SingularityEmailType.REQUEST_IN_COOLDOWN);

    if (emailDestination.isEmpty()) {
      LOG.debug("Not configured to send request cooldown mail for {}", request);
      return;
    }

    final Map<String, Object> templateProperties = Maps.newHashMap();
    populateRequestEmailProperties(templateProperties, request, SingularityEmailType.REQUEST_IN_COOLDOWN);

    final String subject = String.format("Request %s has entered system cooldown — Singularity", request.getId());

    templateProperties.put("numFailures", configuration.getCooldownAfterFailures());
    templateProperties.put("cooldownDelayFormat", DurationFormatUtils.formatDurationHMS(TimeUnit.SECONDS.toMillis(configuration.getCooldownMinScheduleSeconds())));
    templateProperties.put("cooldownExpiresFormat", DurationFormatUtils.formatDurationHMS(TimeUnit.MINUTES.toMillis(configuration.getCooldownExpiresAfterMinutes())));

    final String body = Jade4J.render(requestInCooldownTemplate, templateProperties);

    queueMail(emailDestination, request, SingularityEmailType.REQUEST_IN_COOLDOWN, Optional.<String> absent(), subject, body);
  }

  @Override
  public void sendDisasterMail(final SingularityDisastersData disastersData) {
    mailPreparerExecutorService.submit(new Runnable() {

      @Override
      public void run() {
        try {
          prepareDisasterMail(disastersData);
        } catch (Throwable t) {
          LOG.error("While preparing request in disaster mail for {}", disastersData, t);
          exceptionNotifier.notify(String.format("Error preparing cooldown mail (%s)", t.getMessage()), t, ImmutableMap.of("disasterData", disastersData.toString()));
        }
      }
    });
  }

  private void prepareDisasterMail(final SingularityDisastersData disastersData) {
    final List<SingularityEmailDestination> emailDestination = configuration.getSmtpConfigurationOptional().get().getEmailConfiguration().get(SingularityEmailType.DISASTER_DETECTED);
    if (emailDestination.isEmpty() || !emailDestination.contains(SingularityEmailDestination.ADMINS) || smtpConfiguration.getAdmins().isEmpty()) {
      LOG.info("Not configured to send disaster detected mail");
      return;
    }

    final List<String> toList = smtpConfiguration.getAdmins();
    final List<String> ccList = Lists.newArrayList();
    final String subject = String.format("Disaster(s) Detected %s", disastersData.getDisasters());

    final Map<String, Object> templateProperties = Maps.newHashMap();

    templateProperties.put("disasterTypes", disastersData.getDisasters());
    templateProperties.put("stats", mailTemplateHelpers.getJadeDisasterStats(disastersData.getStats()));

    final String body = Jade4J.render(disastersTemplate, templateProperties);

    smtpSender.queueMail(toList, ccList, subject, body);
  }

  private enum RateLimitStatus {
    RATE_LIMITING_DISABLED, RATE_LIMITED, NOT_RATE_LIMITED;
  }

  private enum RateLimitResult {
    SEND_MAIL, DONT_SEND_MAIL_IN_COOLDOWN, SEND_COOLDOWN_STARTED_MAIL;
  }

  private RateLimitStatus getCurrentRateLimitForMail(SingularityRequest request, SingularityEmailType emailType) {
    if (smtpConfiguration.getRateLimitAfterNotifications() < 1) {
      LOG.trace("Mail rate limit cooldown disabled");
      return RateLimitStatus.RATE_LIMITING_DISABLED;
    }

    final String requestId = request.getId();
    final String emailTypeName = emailType.name();

    final long now = System.currentTimeMillis();

    final Optional<String> cooldownMarker = metadataManager.getMailCooldownMarker(requestId, emailTypeName);

    if (cooldownMarker.isPresent()) {
      final long cooldownLeft = smtpConfiguration.getRateLimitCooldownMillis() - (now - Long.parseLong(cooldownMarker.get()));

      if (cooldownLeft > 0) {
        LOG.debug("Not sending {} for {} - mail cooldown has {} time left out of {}", emailTypeName, requestId, cooldownLeft, smtpConfiguration.getRateLimitCooldownMillis());
        return RateLimitStatus.RATE_LIMITED;
      }

      metadataManager.removeMailCooldown(requestId, emailTypeName);
    }

    return RateLimitStatus.NOT_RATE_LIMITED;
  }

  private RateLimitResult checkRateLimitForMail(SingularityRequest request, SingularityEmailType emailType) {
    RateLimitStatus currentStatus = getCurrentRateLimitForMail(request, emailType);

    switch (currentStatus) {
      case RATE_LIMITED:
        return RateLimitResult.DONT_SEND_MAIL_IN_COOLDOWN;
      case RATE_LIMITING_DISABLED:
        return RateLimitResult.SEND_MAIL;
      case NOT_RATE_LIMITED:
        break;
    }

    final String requestId = request.getId();
    final String emailTypeName = emailType.name();
    final long now = System.currentTimeMillis();

    metadataManager.saveMailRecord(request, emailType);

    int numInPeriod = 0;

    for (String recentMailRecord : metadataManager.getMailRecords(request.getId(), emailType.name())) {
      if (now - Long.parseLong(recentMailRecord) < smtpConfiguration.getRateLimitPeriodMillis()) {
        numInPeriod++;
      }
    }

    if (numInPeriod > smtpConfiguration.getRateLimitAfterNotifications()) {
      LOG.info("{} for {} sent at least {} times in {}, not sending this mail again for at least {}", emailTypeName, requestId, numInPeriod, smtpConfiguration.getRateLimitAfterNotifications(), smtpConfiguration.getRateLimitCooldownMillis());
      metadataManager.cooldownMail(requestId, emailTypeName);
      return RateLimitResult.SEND_COOLDOWN_STARTED_MAIL;
    }

    return RateLimitResult.SEND_MAIL;
  }

  /**
   * Add needed information to the rate limit email Jade context.
   * @param request SingularityRequest that the rate limit email is about.
   * @param emailType what the email is about.
   * @return template properties to add to the Jade context.
   */
  private Map<String, Object> getRateLimitTemplateProperties(SingularityRequest request, final SingularityEmailType emailType) {
    final Builder<String, Object> templateProperties = ImmutableMap.<String, Object>builder();

    templateProperties.put("singularityRequestLink", mailTemplateHelpers.getSingularityRequestLink(request.getId()));
    templateProperties.put("rateLimitAfterNotifications", Integer.toString(smtpConfiguration.getRateLimitAfterNotifications()));
    templateProperties.put("rateLimitPeriodFormat", DurationFormatUtils.formatDurationHMS(smtpConfiguration.getRateLimitPeriodMillis()));
    templateProperties.put("rateLimitCooldownFormat", DurationFormatUtils.formatDurationHMS(smtpConfiguration.getRateLimitCooldownMillis()));
    templateProperties.put("emailType", emailType.name());
    templateProperties.put("requestId", request.getId());
    templateProperties.put("color", emailType.getColor());

    return templateProperties.build();
  }

  /**
   * Check to see if email should be rate limited, and if so, send a rate limit
   * email notification. Next attempt to email will immediately return.
   *
   * @param destination collection of enum values used to specify who will receive this email.
   * @param request SingularityRequest this email is about.
   * @param emailType what the email is about (e.g. TASK_FAILED).
   * @param actionTaker the user taking the action
   * @param subject the subject line of the email.
   * @param body the body of the email.
   */
  private void queueMail(final Collection<SingularityEmailDestination> destination, final SingularityRequest request, final SingularityEmailType emailType, final Optional<String> actionTaker, String subject, String body) {
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

    // Decide where to send this email.
    if (destination.contains(SingularityEmailDestination.OWNERS) && request.getOwners().isPresent() && !request.getOwners().get().isEmpty()) {
      toList.addAll(request.getOwners().get());
    }

    if (destination.contains(SingularityEmailDestination.ADMINS) && !smtpConfiguration.getAdmins().isEmpty()) {
      if (toList.isEmpty()) {
        toList.addAll(smtpConfiguration.getAdmins());
      } else {
        ccList.addAll(smtpConfiguration.getAdmins());
      }
    }

    if (actionTaker.isPresent() && !Strings.isNullOrEmpty(actionTaker.get())) {
      if (destination.contains(SingularityEmailDestination.ACTION_TAKER)) {
        toList.add(actionTaker.get());
      } else {
        final Iterator<String> i = toList.iterator();
        while (i.hasNext()) {
          if (actionTaker.get().equalsIgnoreCase(i.next())) {
            i.remove();
          }
        }
      }
    }

    smtpSender.queueMail(toList, ccList, subject, body);
  }
}
