package com.hubspot.singularity.scheduler;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Singleton;

import org.dmfs.rfc5545.DateTime;
import org.dmfs.rfc5545.recur.InvalidRecurrenceRuleException;
import org.dmfs.rfc5545.recur.RecurrenceRule;
import org.dmfs.rfc5545.recur.RecurrenceRuleIterator;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.hubspot.singularity.ScheduleType;
import com.hubspot.singularity.SingularityDeployStatistics;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
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
                LOG.info("{} is overdue by {}% (expectedRunTime: {}, warnIfScheduledJobIsRunningPastNextRunPct: {})", taskId, overDuePct, expectedRuntime.get(),
                    configuration.getWarnIfScheduledJobIsRunningPastNextRunPct());

                mailer.sendTaskOverdueMail(taskManager.getTask(taskId), taskId, request.getRequest(), runtime, expectedRuntime.get());

                taskManager.saveNotifiedOverdue(taskId);
            } else {
                LOG.trace("{} is not overdue yet - runtime {}, expected {}, ({}% < {}%)", taskId, runtime, expectedRuntime.get(), overDuePct,
                    configuration.getWarnIfScheduledJobIsRunningPastNextRunPct());
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
            long nextRunAtTimestamp = 0;
            final long now = System.currentTimeMillis();
            try {
                if (request.getRequest().getScheduleTypeSafe() == ScheduleType.RFC5545) {
                    final String schedule = request.getRequest().getSchedule().get();
                    final RecurrenceRule recurrenceRule = new RecurrenceRule(schedule);
                    if (recurrenceRule.isInfinite()) {
                        // set limit at 2100-01-01 00:00:00
                        recurrenceRule.setUntil(new DateTime(2100, 1, 1, 0, 0, 0));
                    }

                    // DTSTART is RFC5545 but NOT in the recur string, but its a nice to have
                    Pattern pattern = Pattern.compile("DTSTART=([0-9]{8}T[0-9]{6})");
                    Matcher matcher = pattern.matcher(schedule);
                    DateTime startDateTime;
                    org.joda.time.DateTime dtStart;
                    if (matcher.find()) {
                        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmss");
                        dtStart = formatter.parseDateTime(matcher.group(1));
                    } else {
                        dtStart = org.joda.time.DateTime.now();
                        dtStart = dtStart.withSecondOfMinute(0);
                    }

                    startDateTime = new DateTime(dtStart.getYear(), (dtStart.getMonthOfYear() - 1), dtStart.getDayOfMonth(),
                        dtStart.getHourOfDay(), dtStart.getMinuteOfHour(), dtStart.getSecondOfMinute());

                    RecurrenceRuleIterator timeIterator = recurrenceRule.iterator(startDateTime);
                    nextRunAtTimestamp = 0;
                    while (timeIterator.hasNext()) {
                        nextRunAtTimestamp = timeIterator.nextMillis();
                        if (nextRunAtTimestamp >= now) {
                            break;
                        } else {
                            nextRunAtTimestamp = 0;
                        }
                    }
                    if (nextRunAtTimestamp <= 0) {
                        String msg = String.format("No next run date found for %s (%s)", taskId, request.getRequest().getScheduleTypeSafe());
                        LOG.warn(msg);
                        exceptionNotifier.notify(msg, ImmutableMap.of("taskId", taskId.toString()));
                        return Optional.absent();
                    }
                } else {
                    final CronExpression cronExpression;
                    cronExpression = new CronExpression(request.getRequest().getQuartzScheduleSafe());

                    final Date startDate = new Date(taskId.getStartedAt());
                    final Date nextRunAtDate = cronExpression.getNextValidTimeAfter(startDate);

                    if (nextRunAtDate == null) {
                        String msg = String.format("No next run date found for %s (%s)", taskId, request.getRequest().getQuartzScheduleSafe());
                        LOG.warn(msg);
                        exceptionNotifier.notify(msg, ImmutableMap.of("taskId", taskId.toString()));
                        return Optional.absent();
                    }
                    nextRunAtTimestamp = nextRunAtDate.getTime();
                }
            } catch (ParseException | InvalidRecurrenceRuleException e) {

                if (request.getRequest().getScheduleTypeSafe() == ScheduleType.RFC5545) {
                    LOG.warn("Unable to parse RFC 5545 RECUR for {} ({})", taskId, request.getRequest().getSchedule().get(), e);
                } else {
                    LOG.warn("Unable to parse cron for {} ({})", taskId, request.getRequest().getQuartzScheduleSafe(), e);
                }
                exceptionNotifier.notify(e, ImmutableMap.of("taskId", taskId.toString()));
                return Optional.absent();
            }

            return Optional.of(nextRunAtTimestamp - taskId.getStartedAt());
        }
    }

}
