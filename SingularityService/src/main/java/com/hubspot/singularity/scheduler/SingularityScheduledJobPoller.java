package com.hubspot.singularity.scheduler;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import com.hubspot.singularity.ScheduleType;
import org.dmfs.rfc5545.recur.InvalidRecurrenceRuleException;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityDeployStatistics;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.helpers.RFC5545Schedule;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;
import com.hubspot.singularity.smtp.SingularityMailer;


@Singleton
public class SingularityScheduledJobPoller extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityScheduledJobPoller.class);

  private final TaskManager taskManager;
  private final RequestManager requestManager;
  private final DeployManager deployManager;
  private final SingularityMailer mailer;
  private final SingularityConfiguration configuration;
  private final SingularityExceptionNotifier exceptionNotifier;

  @Inject
  public SingularityScheduledJobPoller(SingularityExceptionNotifier exceptionNotifier, TaskManager taskManager,
      SingularityConfiguration configuration, RequestManager requestManager, DeployManager deployManager, SingularityMailer mailer) {

    super(configuration.getCheckScheduledJobsEveryMillis(), TimeUnit.MILLISECONDS);

    this.taskManager = taskManager;
    this.deployManager = deployManager;
    this.configuration = configuration;
    this.exceptionNotifier = exceptionNotifier;
    this.requestManager = requestManager;
    this.mailer = mailer;
  }

  @Override
  public void runActionOnPoll() {
    final long start = System.currentTimeMillis();

    final List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIds();
    final Set<String> requestIdsToLookup = Sets.newHashSetWithExpectedSize(activeTaskIds.size());

    for (SingularityTaskId taskId : activeTaskIds) {
      if (start - taskId.getStartedAt() < configuration.getWarnIfScheduledJobIsRunningForAtLeastMillis()) {
        continue;
      }

      requestIdsToLookup.add(taskId.getRequestId());
    }

    final List<SingularityRequestWithState> requests = requestManager.getRequests(requestIdsToLookup);
    final Map<String, SingularityRequestWithState> idToRequest = Maps.uniqueIndex(requests, SingularityRequestWithState.REQUEST_STATE_TO_REQUEST_ID);

    for (SingularityTaskId taskId : activeTaskIds) {
      SingularityRequestWithState request = idToRequest.get(taskId.getRequestId());

      if (request == null || !request.getRequest().isScheduled() || taskManager.hasNotifiedOverdue(taskId)) {
        continue;
      }

      final long runtime = start - taskId.getStartedAt();
      final Optional<Long> expectedRuntime = getExpectedRuntime(request, taskId);

      if (!expectedRuntime.isPresent()) {
        continue;
      }

      final int overDuePct = (int) (100 * (runtime / (float) expectedRuntime.get()));

      if (overDuePct > configuration.getWarnIfScheduledJobIsRunningPastNextRunPct()) {
        LOG.info("{} is overdue by {}% (expectedRunTime: {}, warnIfScheduledJobIsRunningPastNextRunPct: {})", taskId, overDuePct, expectedRuntime.get(), configuration.getWarnIfScheduledJobIsRunningPastNextRunPct());

        mailer.sendTaskOverdueMail(taskManager.getTask(taskId), taskId, request.getRequest(), runtime, expectedRuntime.get());

        taskManager.saveNotifiedOverdue(taskId);
      } else {
        LOG.trace("{} is not overdue yet - runtime {}, expected {}, ({}% < {}%)", taskId, runtime, expectedRuntime.get(), overDuePct, configuration.getWarnIfScheduledJobIsRunningPastNextRunPct());
      }
    }
  }

  private Optional<Long> getExpectedRuntime(SingularityRequestWithState request, SingularityTaskId taskId) {
    if (request.getRequest().getScheduledExpectedRuntimeMillis().isPresent()) {
      return request.getRequest().getScheduledExpectedRuntimeMillis();
    } else {
      final Optional<SingularityDeployStatistics> deployStatistics = deployManager.getDeployStatistics(taskId.getRequestId(), taskId.getDeployId());

      if (deployStatistics.isPresent() && deployStatistics.get().getAverageRuntimeMillis().isPresent()) {
        return deployStatistics.get().getAverageRuntimeMillis();
      }
      Date nextRunAtDate = null;
      long nextRunAtTimestamp = 0;
      String scheduleExpression = null;
      try {
        if(request.getRequest().getScheduleTypeSafe() == ScheduleType.RFC5545)
        {
          scheduleExpression = request.getRequest().getSchedule().get();
          final RFC5545Schedule rfc5545Schedule = new RFC5545Schedule(scheduleExpression);
          nextRunAtDate = rfc5545Schedule.getNextValidTime();
        }
        else {
          scheduleExpression = request.getRequest().getQuartzScheduleSafe();
          final CronExpression cronExpression;
          cronExpression = new CronExpression(scheduleExpression);
          final Date startDate = new Date(taskId.getStartedAt());
          nextRunAtDate = cronExpression.getNextValidTimeAfter(startDate);
        }

        if (nextRunAtDate == null) {
          String msg = String.format("No next run date found for %s (%s)", taskId, scheduleExpression);
          LOG.warn(msg);
          exceptionNotifier.notify(msg, ImmutableMap.of("taskId", taskId.toString()));
          return Optional.absent();
        }
        nextRunAtTimestamp = nextRunAtDate.getTime();

      } catch (ParseException|InvalidRecurrenceRuleException e) {
        if(request.getRequest().getScheduleTypeSafe() == ScheduleType.RFC5545) {
          LOG.warn("Unable to parse RFC5545 RECUR for {} ({})", scheduleExpression, e);
        }
        else {
          LOG.warn("Unable to parse cron for {} ({})", taskId, scheduleExpression, e);
        }
        exceptionNotifier.notify(e, ImmutableMap.of("taskId", taskId.toString()));
        return Optional.absent();
      }

      return Optional.of(nextRunAtTimestamp - taskId.getStartedAt());
    }
  }

}
