package com.hubspot.singularity.scheduler;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.mesos.SingularityMesosSchedulerDelegator;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;
import com.hubspot.singularity.smtp.SingularityMailer;

@Singleton
public class SingularityScheduledJobPoller extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityScheduledJobPoller.class);

  private final TaskManager taskManager;
  private final RequestManager requestManager;
  private final SingularityMailer mailer;
  private final SingularityConfiguration configuration;
  private final SingularityExceptionNotifier exceptionNotifier;

  @Inject
  public SingularityScheduledJobPoller(final LeaderLatch leaderLatch, final SingularityMesosSchedulerDelegator mesosScheduler, SingularityExceptionNotifier exceptionNotifier, TaskManager taskManager,
      SingularityConfiguration configuration, SingularityAbort abort, RequestManager requestManager, SingularityMailer mailer) {
    super(leaderLatch, mesosScheduler, exceptionNotifier, abort, configuration.getCheckScheduledJobsEveryMillis(), TimeUnit.MILLISECONDS, SchedulerLockType.LOCK);

    this.taskManager = taskManager;
    this.configuration = configuration;
    this.exceptionNotifier = exceptionNotifier;
    this.requestManager = requestManager;
    this.mailer = mailer;
  }

  @Override
  public void runActionOnPoll() {
    final long start = System.currentTimeMillis();

    final List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIds();
    final List<String> requestIdsToLookup = Lists.newArrayListWithCapacity(activeTaskIds.size());

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

      if (request == null || !request.getRequest().isScheduled()) {
        continue;
      }

      final CronExpression cronExpression;

      try {
        cronExpression = new CronExpression(request.getRequest().getQuartzScheduleSafe());
      } catch (ParseException e) {
        LOG.warn("Unable to parse cron for {} ({})", taskId, request.getRequest().getQuartzScheduleSafe(), e);
        exceptionNotifier.notify(e);
        continue;
      }

      final Date startDate = new Date(taskId.getStartedAt());
      final Date nextRunAtDate = cronExpression.getNextValidTimeAfter(startDate);

      if (nextRunAtDate == null) {
        String msg = String.format("No next run date found for %s (%s)", taskId, request.getRequest().getQuartzScheduleSafe());
        LOG.warn(msg);
        exceptionNotifier.notify(msg);
        continue;
      }

      final long period = nextRunAtDate.getTime() - taskId.getStartedAt();
      final long overDueBy = start - nextRunAtDate.getTime();

      final float overDuePct = (float) overDueBy / (float) period;

      if (overDuePct > configuration.getWarnIfScheduledJobIsRunningPastNextRunPct()) {
        LOG.info("{} is overdue by {} (period: {}, warnIfScheduledJobIsRunningPastNextRunPct: {})", taskId, overDuePct, period, configuration.getWarnIfScheduledJobIsRunningPastNextRunPct());



        // send mail TODO
      }
    }
  }
}
