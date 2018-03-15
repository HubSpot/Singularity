package com.hubspot.singularity.scheduler;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.dmfs.rfc5545.recur.InvalidRecurrenceRuleException;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.hubspot.singularity.api.deploy.SingularityDeployStatistics;
import com.hubspot.singularity.api.request.ScheduleType;
import com.hubspot.singularity.api.request.SingularityRequest;
import com.hubspot.singularity.api.request.SingularityRequestWithState;
import com.hubspot.singularity.api.task.SingularityTaskCleanup;
import com.hubspot.singularity.api.task.SingularityTaskId;
import com.hubspot.singularity.api.task.TaskCleanupType;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.helpers.RFC5545Schedule;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;
import com.hubspot.singularity.smtp.SingularityMailer;

@Singleton
public class SingularityJobPoller extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityJobPoller.class);

  private final TaskManager taskManager;
  private final RequestManager requestManager;
  private final DeployManager deployManager;
  private final SingularityMailer mailer;
  private final SingularityConfiguration configuration;
  private final SingularityExceptionNotifier exceptionNotifier;

  @Inject
  public SingularityJobPoller(SingularityExceptionNotifier exceptionNotifier, TaskManager taskManager,
                              SingularityConfiguration configuration, RequestManager requestManager, DeployManager deployManager, SingularityMailer mailer) {

    super(configuration.getCheckJobsEveryMillis(), TimeUnit.MILLISECONDS);

    this.taskManager = taskManager;
    this.deployManager = deployManager;
    this.configuration = configuration;
    this.exceptionNotifier = exceptionNotifier;
    this.requestManager = requestManager;
    this.mailer = mailer;
  }

  @Override
  public void runActionOnPoll() {
    final long now = System.currentTimeMillis();

    final List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIds();
    final Set<String> requestIdsToLookup = Sets.newHashSetWithExpectedSize(activeTaskIds.size());

    for (SingularityTaskId taskId : activeTaskIds) {
      requestIdsToLookup.add(taskId.getRequestId());
    }

    final Map<String, SingularityRequestWithState> idToRequest = Maps.uniqueIndex(requestManager.getRequests(requestIdsToLookup), SingularityRequestWithState.REQUEST_STATE_TO_REQUEST_ID);

    for (SingularityTaskId taskId : activeTaskIds) {
      SingularityRequestWithState requestWithState = idToRequest.get(taskId.getRequestId());

      if (requestWithState == null) {
        LOG.warn("Active request not found for task ID {}", taskId);
        continue;
      }

      SingularityRequest request = requestWithState.getRequest();
      if (!request.isLongRunning()) {
        checkForOverdueScheduledJob(now - taskId.getStartedAt(), taskId, request);
        checkTaskExecutionTimeLimit(now, taskId, request);
      }
    }
  }

  private void checkForOverdueScheduledJob(long runtime, SingularityTaskId taskId, SingularityRequest request) {
    if (request.isScheduled() &&
        !taskManager.hasNotifiedOverdue(taskId) &&
        runtime >= configuration.getWarnIfScheduledJobIsRunningForAtLeastMillis()) {
      final Optional<Long> expectedRuntime = getExpectedRuntime(request, taskId);

      if (expectedRuntime.isPresent()) {

        final int overDuePct = (int) (100 * (runtime / (float) expectedRuntime.get()));

        if (overDuePct > configuration.getWarnIfScheduledJobIsRunningPastNextRunPct()) {
          LOG.info("{} is overdue by {}% (expectedRunTime: {}, warnIfScheduledJobIsRunningPastNextRunPct: {})", taskId, overDuePct, expectedRuntime.get(), configuration.getWarnIfScheduledJobIsRunningPastNextRunPct());

          mailer.sendTaskOverdueMail(taskManager.getTask(taskId), taskId, request, runtime, expectedRuntime.get());

          taskManager.saveNotifiedOverdue(taskId);
        } else {
          LOG.trace("{} is not overdue yet - runtime {}, expected {}, ({}% < {}%)", taskId, runtime, expectedRuntime.get(), overDuePct, configuration.getWarnIfScheduledJobIsRunningPastNextRunPct());
        }
      }
    }
  }

  private void checkTaskExecutionTimeLimit(long now, SingularityTaskId taskId, SingularityRequest request) {
    final long runtime = now - taskId.getStartedAt();

    if ((request.getTaskExecutionTimeLimitMillis().isPresent() || configuration.getTaskExecutionTimeLimitMillis().isPresent()) &&
        runtime >= (request.getTaskExecutionTimeLimitMillis().isPresent() ? request.getTaskExecutionTimeLimitMillis().get() : configuration.getTaskExecutionTimeLimitMillis().get())) {

      taskManager.createTaskCleanup(new SingularityTaskCleanup(
          Optional.empty(),
          TaskCleanupType.TASK_EXCEEDED_TIME_LIMIT,
          now,
          taskId,
          Optional.of(String.format("Task has run for %s, which exceeds the maximum execution time of %s",
              DurationFormatUtils.formatDurationHMS(runtime),
              DurationFormatUtils.formatDurationHMS(request.getTaskExecutionTimeLimitMillis().map(Optional::of).orElse(configuration.getTaskExecutionTimeLimitMillis()).get()))
          ),
          Optional.of(UUID.randomUUID().toString()),
          Optional.empty(),
          Optional.empty())
      );
    }
  }

  private Optional<Long> getExpectedRuntime(SingularityRequest request, SingularityTaskId taskId) {
    if (request.getScheduledExpectedRuntimeMillis().isPresent()) {
      return request.getScheduledExpectedRuntimeMillis();
    } else {
      final Optional<SingularityDeployStatistics> deployStatistics = deployManager.getDeployStatistics(taskId.getRequestId(), taskId.getDeployId());

      if (deployStatistics.isPresent() && deployStatistics.get().getAverageRuntimeMillis().isPresent()) {
        return deployStatistics.get().getAverageRuntimeMillis();
      }

      String scheduleExpression = request.getScheduleTypeSafe() == ScheduleType.RFC5545 ? request.getSchedule().get() : request.getQuartzScheduleSafe();
      Date nextRunAtDate;

      try {
        if (request.getScheduleTypeSafe() == ScheduleType.RFC5545) {
          final RFC5545Schedule rfc5545Schedule = new RFC5545Schedule(scheduleExpression);
          nextRunAtDate = rfc5545Schedule.getNextValidTime();
        } else {
          final CronExpression cronExpression = new CronExpression(scheduleExpression);
          final Date startDate = new Date(taskId.getStartedAt());
          nextRunAtDate = cronExpression.getNextValidTimeAfter(startDate);
        }

        if (nextRunAtDate == null) {
          String msg = String.format("No next run date found for %s (%s)", taskId, scheduleExpression);
          LOG.warn(msg);
          exceptionNotifier.notify(msg, ImmutableMap.of("taskId", taskId.toString()));
          return Optional.empty();
        }

      } catch (ParseException|InvalidRecurrenceRuleException e) {
        LOG.warn("Unable to parse schedule of type {} for expression {} (taskId: {}, err: {})", request.getScheduleTypeSafe(), scheduleExpression, taskId, e);
        exceptionNotifier.notify(String.format("Unable to parse schedule (%s)", e.getMessage()), e, ImmutableMap.of("taskId", taskId.toString(), "scheduleExpression", scheduleExpression, "scheduleType", request.getScheduleTypeSafe().toString()));
        return Optional.empty();
      }

      return Optional.of(nextRunAtDate.getTime() - taskId.getStartedAt());
    }
  }

}
