package com.hubspot.singularity.notifications;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityDisastersData;
import com.hubspot.singularity.SingularityNotificationType;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.api.SingularityPauseRequest;
import com.hubspot.singularity.api.SingularityScaleRequest;
import com.hubspot.singularity.config.SlackConfiguration;
import com.hubspot.singularity.smtp.MailTemplateHelpers;

import io.dropwizard.lifecycle.Managed;

@Singleton
public class SlackNotifier implements SingularityNotifier, Managed {

  private static final Logger LOG = LoggerFactory.getLogger(SlackNotifier.class);

  private final SlackClient slackClient;
  private final NotificationHelper notificationHelper;
  private final MailTemplateHelpers mailTemplateHelpers;
  private final ExecutorService slackExecutor;

  @Inject
  public SlackNotifier(
      SlackConfiguration slackConfiguration,
      SlackClient slackClient,
      NotificationHelper notificationHelper,
      MailTemplateHelpers mailTemplateHelpers
  ) throws IOException {
    this.slackClient = slackClient;
    this.notificationHelper = notificationHelper;
    this.mailTemplateHelpers = mailTemplateHelpers;
    this.slackExecutor = JavaUtils.newFixedTimingOutThreadPool(
        slackConfiguration.getSlackMaxThreads(),
        TimeUnit.SECONDS.toMillis(1),
        "SingularitySlackNotifier-%d"
    );
  }

  @Override
  public void sendTaskOverdueNotification(Optional<SingularityTask> task, SingularityTaskId taskId, SingularityRequest request, long runTime, long expectedRuntime) {
    String duration = String.format("%d:%02d:%02d", runTime / 3600, (runTime % 3600) / 60, (runTime % 60));
    postToSlack(
        Optional.of(taskId),
        request,
        SingularityNotificationType.TASK_SCHEDULED_OVERDUE_TO_FINISH,
        String.format("%s has been running for %s and is overdue to finish", request.getId(), duration)
    );
  }

  @Override
  public void sendTaskFinishedNotification(
      SingularityTaskHistory taskHistory,
      SingularityRequest request
  ) {
    SingularityTaskId taskId = taskHistory.getTask().getTaskId();
    Optional<SingularityNotificationType> notificationType = notificationHelper.getNotificationType(taskHistory, request);
    if (!notificationType.isPresent()) {
      LOG.warn("Slack notifications are not configured for {} {}", request.getId(), taskHistory.getLastTaskUpdate().get().getTaskState());
      return;
    }
    postToSlack(
        Optional.of(taskId),
        request,
        notificationType.get(),
        String.format("%s has finished", request.getId())
    );
  }

  @Override
  public void sendRequestPausedNotification(
      SingularityRequest request,
      Optional<SingularityPauseRequest> pauseRequest,
      Optional<String> user
  ) {
    postToSlack(
        Optional.absent(),
        request,
        SingularityNotificationType.REQUEST_PAUSED,
        String.format("%s has been paused by %s", request.getId(), user.isPresent() ? user.get() : "[unknown user]")
    );

  }

  @Override
  public void sendRequestUnpausedNotification(SingularityRequest request, Optional<String> user, Optional<String> message) {
    postToSlack(
        Optional.absent(),
        request,
        SingularityNotificationType.REQUEST_UNPAUSED,
        String.format("%s has been un-paused by %s", request.getId(), user.isPresent() ? user.get() : "[unknown user]")
    );
  }

  @Override
  public void sendRequestScaledNotification(
      SingularityRequest request,
      Optional<SingularityScaleRequest> newScaleRequest,
      Optional<Integer> formerInstances,
      Optional<String> user
  ) {
    String fromInstanceCount = formerInstances.isPresent() ? String.valueOf(formerInstances.get()) : "[unknown quantity]";
    String toInstanceCount = newScaleRequest.isPresent() && newScaleRequest.get().getInstances().isPresent()
        ? String.valueOf(newScaleRequest.get().getInstances().get())
        : "[unknown quantity]";
    postToSlack(
        Optional.absent(),
        request,
        SingularityNotificationType.REQUEST_SCALED,
        String.format("%s is being scaled from %s to %s by %s",
            request.getId(),
            fromInstanceCount,
            toInstanceCount,
            user.isPresent() ? user.get() : "[unknown user]"));
  }

  @Override
  public void sendRequestRemovedNotification(SingularityRequest request, Optional<String> user, Optional<String> message) {
    postToSlack(
        Optional.absent(),
        request,
        SingularityNotificationType.REQUEST_REMOVED,
        String.format("%s has been removed by %s", request.getId(), user.isPresent() ? user.get() : "[unknown user]")
    );
  }

  @Override
  public void sendRequestInCooldownNotification(SingularityRequest request) {
    postToSlack(
        Optional.absent(),
        request,
        SingularityNotificationType.REQUEST_IN_COOLDOWN,
        String.format("%s has entered system cooldown", request.getId())
    );
  }

  @Override
  public void sendDisasterNotification(SingularityDisastersData disastersData) {
    LOG.debug("Disaster notifications not yet implemented for Slack");
  }

  @Override
  public boolean shouldNotify(
      SingularityRequest request
  ) {
    return request.getSlackConfigurationOverrides().isPresent();
  }

  @Override
  public void start() throws Exception {
    slackClient.start();
  }

  @Override
  public void stop() throws Exception {
    slackClient.stop();
  }

  private String getSingularityUrl(SingularityTaskId taskId) {
    return mailTemplateHelpers.getSingularityTaskLink(taskId.getId());
  }

  private String getSingularityUrl(SingularityRequest request) {
    return mailTemplateHelpers.getSingularityRequestLink(request.getId());
  }

  private void postToSlack(
      Optional<SingularityTaskId> taskId,
      SingularityRequest request,
      SingularityNotificationType notificationType,
      String message
  ) {
    Optional<Map<SingularityNotificationType, List<String>>> maybeOverrides = request.getSlackConfigurationOverrides();
    if (maybeOverrides.isPresent()) {
      List<String> channels = maybeOverrides.get().get(notificationType);
      slackExecutor.submit(() -> slackClient.broadcastMessage(
          channels,
          "%s (%s)",
          message,
          taskId.isPresent() ? getSingularityUrl(taskId.get()) : getSingularityUrl(request)));
    }
  }
}
