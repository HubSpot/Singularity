package com.hubspot.singularity.scheduler;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityEmailDestination;
import com.hubspot.singularity.SingularityNotificationType;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.data.DisasterManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.notifications.NotificationHelper;
import com.hubspot.singularity.notifications.RateLimitHelper;
import com.hubspot.singularity.notifications.RateLimitStatus;
import com.hubspot.singularity.smtp.MailTemplateHelpers;

@Singleton
public class SingularityNotificationScheduler {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityNotificationScheduler.class);

  private final TaskManager taskManager;
  private final NotificationHelper notificationHelper;
  private final MailTemplateHelpers mailTemplateHelpers;
  private final RateLimitHelper rateLimitHelper;
  private final DisasterManager disasterManager;

  @Inject
  public SingularityNotificationScheduler(
      TaskManager taskManager,
      NotificationHelper notificationHelper,
      MailTemplateHelpers mailTemplateHelpers,
      RateLimitHelper rateLimitHelper,
      DisasterManager disasterManager
  ) {
    this.taskManager = taskManager;
    this.notificationHelper = notificationHelper;
    this.mailTemplateHelpers = mailTemplateHelpers;
    this.rateLimitHelper = rateLimitHelper;
    this.disasterManager = disasterManager;
  }

  public void queueTaskCompletedNotification(
      SingularityTaskId taskId,
      SingularityRequest request,
      ExtendedTaskState state
  ) {
    if (shouldQueueNotificationInZk(taskId, request, state)) {
      taskManager.saveTaskFinishedInNotificationQueue(taskId);
    }
  }

  private boolean shouldQueueNotificationInZk(
      SingularityTaskId taskId,
      SingularityRequest request,
      ExtendedTaskState taskState
  ) {
    final Collection<SingularityTaskHistoryUpdate> taskHistory = taskManager.getTaskHistoryUpdates(taskId);
    final Optional<SingularityNotificationType> notificationType = notificationHelper.getNotificationType(taskState, request, taskHistory);

    if (!notificationType.isPresent()) {
      LOG.debug("No configured emailType for {} and {}", request, taskState);
      return false;
    }

    final Collection<SingularityEmailDestination> emailDestination = mailTemplateHelpers.getDestination(request, notificationType.get());

    if (emailDestination.isEmpty()) {
      LOG.debug("Not configured to send task emails for {}", notificationType);
      return false;
    }

    if (disasterManager.isDisabled(SingularityAction.SEND_EMAIL)) {
      LOG.debug("Not sending notification because SEND_EMAIL action is disabled.");
      return false;
    }

    RateLimitStatus rateLimitStatus = rateLimitHelper.getCurrentRateLimitForMail(request, notificationType.get());

    switch (rateLimitStatus) {
      case RATE_LIMITED:
        return false;
      case RATE_LIMITING_DISABLED:
      case NOT_RATE_LIMITED:
      default:
        return true;
    }
  }

}
