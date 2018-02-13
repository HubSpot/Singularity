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
import com.hubspot.singularity.SingularityDisastersData;
import com.hubspot.singularity.SingularityEmailDestination;
import com.hubspot.singularity.SingularityMainModule;
import com.hubspot.singularity.SingularityNotificationType;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskMetadata;
import com.hubspot.singularity.api.SingularityPauseRequest;
import com.hubspot.singularity.api.SingularityScaleRequest;
import com.hubspot.singularity.config.SMTPConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.MetadataManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.notifications.NotificationHelper;
import com.hubspot.singularity.notifications.RateLimitHelper;
import com.hubspot.singularity.notifications.RateLimitResult;
import com.hubspot.singularity.notifications.SingularityNotifier;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

import de.neuland.jade4j.Jade4J;
import de.neuland.jade4j.template.JadeTemplate;
import io.dropwizard.lifecycle.Managed;

public class SmtpMailNotifier implements SingularityNotifier, Managed {

  private static final Logger LOG = LoggerFactory.getLogger(SmtpMailNotifier.class);

  private final SingularitySmtpSender smtpSender;
  private final SingularityConfiguration configuration;
  private final SMTPConfiguration conf;
  private final ThreadPoolExecutor mailPreparerExecutorService;
  private final SingularityExceptionNotifier exceptionNotifier;

  private final NotificationHelper notificationHelper;
  private final TaskManager taskManager;
  private final RateLimitHelper rateLimitHelper;

  private final JadeTemplate taskTemplate;
  private final JadeTemplate requestInCooldownTemplate;
  private final JadeTemplate requestModifiedTemplate;
  private final JadeTemplate rateLimitedTemplate;
  private final JadeTemplate disastersTemplate;

  private final Joiner adminJoiner;
  private final MailTemplateHelpers mailTemplateHelpers;

  @Inject
  public SmtpMailNotifier(
      SingularitySmtpSender smtpSender,
      SingularityConfiguration configuration,
      NotificationHelper notificationHelper,
      TaskManager taskManager,
      SingularityExceptionNotifier exceptionNotifier,
      MailTemplateHelpers mailTemplateHelpers,
      RateLimitHelper rateLimitHelper,
      @Named(SingularityMainModule.TASK_TEMPLATE) JadeTemplate taskTemplate,
      @Named(SingularityMainModule.REQUEST_IN_COOLDOWN_TEMPLATE) JadeTemplate requestInCooldownTemplate,
      @Named(SingularityMainModule.REQUEST_MODIFIED_TEMPLATE) JadeTemplate requestModifiedTemplate,
      @Named(SingularityMainModule.RATE_LIMITED_TEMPLATE) JadeTemplate rateLimitedTemplate,
      @Named(SingularityMainModule.DISASTERS_TEMPLATE) JadeTemplate disastersTemplate) {

    this.smtpSender = smtpSender;
    this.conf = configuration.getSmtpConfigurationOptional().get();
    this.configuration = configuration;
    this.notificationHelper = notificationHelper;
    this.taskManager = taskManager;
    this.rateLimitHelper = rateLimitHelper;
    this.exceptionNotifier = exceptionNotifier;
    this.adminJoiner = Joiner.on(", ").skipNulls();

    this.mailTemplateHelpers = mailTemplateHelpers;

    this.requestModifiedTemplate = requestModifiedTemplate;
    this.taskTemplate = taskTemplate;
    this.requestInCooldownTemplate = requestInCooldownTemplate;
    this.rateLimitedTemplate = rateLimitedTemplate;
    this.disastersTemplate = disastersTemplate;

    this.mailPreparerExecutorService = JavaUtils.newFixedTimingOutThreadPool(conf.getMailMaxThreads(), TimeUnit.SECONDS.toMillis(1), "SingularityMailPreparer-%d");
  }

  @Override
  public void start() throws Exception {
  }

  @Override
  public void stop() throws Exception {
    MoreExecutors.shutdownAndAwaitTermination(mailPreparerExecutorService, 1, TimeUnit.SECONDS);
  }

  private void populateRequestEmailProperties(Map<String, Object> templateProperties, SingularityRequest request, SingularityNotificationType emailType) {
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

  private void populateTaskEmailProperties(Map<String, Object> templateProperties, SingularityTaskId taskId, Collection<SingularityTaskHistoryUpdate> taskHistory, ExtendedTaskState taskState, List<SingularityTaskMetadata> taskMetadata, SingularityNotificationType emailType) {
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

  @Override
  public void sendTaskOverdueNotification(final Optional<SingularityTask> task, final SingularityTaskId taskId, final SingularityRequest request, final long runTime, final long expectedRuntime) {
    final Builder<String, Object> templateProperties = ImmutableMap.<String, Object>builder();

    templateProperties.put("runTime", DurationFormatUtils.formatDurationHMS(runTime));
    templateProperties.put("expectedRunTime", DurationFormatUtils.formatDurationHMS(expectedRuntime));
    templateProperties.put("warningThreshold", String.format("%s%%", configuration.getWarnIfScheduledJobIsRunningPastNextRunPct()));

    templateProperties.put("status", "is overdue to finish");

    prepareTaskMail(task, taskId, request, SingularityNotificationType.TASK_SCHEDULED_OVERDUE_TO_FINISH, templateProperties.build(), taskManager.getTaskHistoryUpdates(taskId),
        ExtendedTaskState.TASK_RUNNING, Collections.<SingularityTaskMetadata> emptyList());
  }

  @Override
  public boolean shouldNotify(SingularityRequest request) {
    return request.getEmailConfigurationOverrides().isPresent();
  }

  private void prepareTaskMail(Optional<SingularityTask> task, SingularityTaskId taskId, SingularityRequest request, SingularityNotificationType emailType, Map<String, Object> extraProperties,
                               Collection<SingularityTaskHistoryUpdate> taskHistory, ExtendedTaskState taskState, List<SingularityTaskMetadata> taskMetadata) {
    final Collection<SingularityEmailDestination> emailDestination = mailTemplateHelpers.getDestination(request, emailType);

    final Map<String, Object> templateProperties = Maps.newHashMap();
    populateRequestEmailProperties(templateProperties, request, emailType);
    populateTaskEmailProperties(templateProperties, taskId, taskHistory, taskState, taskMetadata, emailType);
    templateProperties.putAll(extraProperties);

    final String subject = mailTemplateHelpers.getSubjectForTaskHistory(taskId, taskState, emailType, taskHistory);

    final String adminEmails = adminJoiner.join(conf.getAdmins());
    templateProperties.put("adminEmails", adminEmails);

    final String body = Jade4J.render(taskTemplate, templateProperties);

    final Optional<String> user = task.isPresent() ? task.get().getTaskRequest().getPendingTask().getUser() : Optional.<String> absent();

    queueMail(emailDestination, request, emailType, user, subject, body);
  }

  @Override
  public void sendTaskFinishedNotification(SingularityTaskHistory taskHistory, SingularityRequest request) {

    Optional<SingularityNotificationType> notificationType =
        notificationHelper.getNotificationType(taskHistory, request);

    ExtendedTaskState lastTaskState = taskHistory.getLastTaskUpdate().get().getTaskState();
    if (!notificationType.isPresent()) {
      LOG.debug("No configured notifications for {} and {}", request, lastTaskState);
      return;
    }

    prepareTaskMail(Optional.of(taskHistory.getTask()), taskHistory.getTask().getTaskId(), request, notificationType.get(), Collections.<String, Object> emptyMap(),
        taskHistory.getTaskUpdates(), lastTaskState, taskHistory.getTaskMetadata());
  }

  private enum RequestMailType {

    PAUSED(SingularityNotificationType.REQUEST_PAUSED), UNPAUSED(SingularityNotificationType.REQUEST_UNPAUSED), REMOVED(SingularityNotificationType.REQUEST_REMOVED), SCALED(SingularityNotificationType.REQUEST_SCALED);

    private final SingularityNotificationType emailType;

    RequestMailType(SingularityNotificationType emailType) {
      this.emailType = emailType;
    }

    public SingularityNotificationType getEmailType() {
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
    final List<SingularityEmailDestination> emailDestination = mailTemplateHelpers.getDestination(request, type.getEmailType());

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
    additionalProperties.put("expireFormat", DateFormatUtils.format(new Date(future), conf.getMailerDatePattern(), conf.getMailerTimeZone()));
  }

  @Override
  public void sendRequestPausedNotification(SingularityRequest request, Optional<SingularityPauseRequest> pauseRequest, Optional<String> user) {
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
  public void sendRequestUnpausedNotification(SingularityRequest request, Optional<String> user, Optional<String> message) {
    sendRequestMail(request, RequestMailType.UNPAUSED, user, message, Optional.<Map<String, Object>> absent());
  }

  @Override
  public void sendRequestScaledNotification(SingularityRequest request, Optional<SingularityScaleRequest> newScaleRequest, Optional<Integer> formerInstances, Optional<String> user) {
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
  public void sendRequestRemovedNotification(SingularityRequest request, Optional<String> user, Optional<String> message) {
    sendRequestMail(request, RequestMailType.REMOVED, user, message, Optional.<Map<String, Object>> absent());
  }

  @Override
  public void sendRequestInCooldownNotification(final SingularityRequest request) {
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
    final List<SingularityEmailDestination> emailDestination = mailTemplateHelpers.getDestination(request, SingularityNotificationType.REQUEST_IN_COOLDOWN);

    if (emailDestination.isEmpty()) {
      LOG.debug("Not configured to send request cooldown mail for {}", request);
      return;
    }

    final Map<String, Object> templateProperties = Maps.newHashMap();
    populateRequestEmailProperties(templateProperties, request, SingularityNotificationType.REQUEST_IN_COOLDOWN);

    final String subject = String.format("Request %s has entered system cooldown — Singularity", request.getId());

    templateProperties.put("numFailures", configuration.getCooldownAfterFailures());
    templateProperties.put("cooldownDelayFormat", DurationFormatUtils.formatDurationHMS(TimeUnit.SECONDS.toMillis(configuration.getCooldownMinScheduleSeconds())));
    templateProperties.put("cooldownExpiresFormat", DurationFormatUtils.formatDurationHMS(TimeUnit.MINUTES.toMillis(configuration.getCooldownExpiresAfterMinutes())));

    final String body = Jade4J.render(requestInCooldownTemplate, templateProperties);

    queueMail(emailDestination, request, SingularityNotificationType.REQUEST_IN_COOLDOWN, Optional.<String> absent(), subject, body);
  }

  @Override
  public void sendDisasterNotification(final SingularityDisastersData disastersData) {
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
    final List<SingularityEmailDestination> emailDestination = configuration.getSmtpConfigurationOptional().get().getEmailConfiguration().get(SingularityNotificationType.DISASTER_DETECTED);
    if (emailDestination.isEmpty() || !emailDestination.contains(SingularityEmailDestination.ADMINS) || conf.getAdmins().isEmpty()) {
      LOG.info("Not configured to send disaster detected mail");
      return;
    }

    final List<String> toList = conf.getAdmins();
    final List<String> ccList = Lists.newArrayList();
    final String subject = String.format("Disaster(s) Detected %s", disastersData.getDisasters());

    final Map<String, Object> templateProperties = Maps.newHashMap();

    templateProperties.put("disasterTypes", disastersData.getDisasters());
    templateProperties.put("stats", mailTemplateHelpers.getJadeDisasterStats(disastersData.getStats()));

    final String body = Jade4J.render(disastersTemplate, templateProperties);

    smtpSender.queueMail(toList, ccList, subject, body);
  }

  /**
   * Add needed information to the rate limit email Jade context.
   * @param request SingularityRequest that the rate limit email is about.
   * @param emailType what the email is about.
   * @return template properties to add to the Jade context.
   */
  private Map<String, Object> getRateLimitTemplateProperties(SingularityRequest request, final SingularityNotificationType emailType) {
    final Builder<String, Object> templateProperties = ImmutableMap.builder();

    templateProperties.put("singularityRequestLink", mailTemplateHelpers.getSingularityRequestLink(request.getId()));
    templateProperties.put("rateLimitAfterNotifications", Integer.toString(conf.getRateLimitAfterNotifications()));
    templateProperties.put("rateLimitPeriodFormat", DurationFormatUtils.formatDurationHMS(conf.getRateLimitPeriodMillis()));
    templateProperties.put("rateLimitCooldownFormat", DurationFormatUtils.formatDurationHMS(conf.getRateLimitCooldownMillis()));
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
  private void queueMail(final Collection<SingularityEmailDestination> destination, final SingularityRequest request, final SingularityNotificationType emailType, final Optional<String> actionTaker, String subject, String body) {
    RateLimitResult result = rateLimitHelper.checkRateLimitForMail(request, emailType);

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

    if (destination.contains(SingularityEmailDestination.ADMINS) && !conf.getAdmins().isEmpty()) {
      if (toList.isEmpty()) {
        toList.addAll(conf.getAdmins());
      } else {
        ccList.addAll(conf.getAdmins());
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
