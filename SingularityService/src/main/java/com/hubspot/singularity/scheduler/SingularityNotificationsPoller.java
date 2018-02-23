package com.hubspot.singularity.scheduler;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskMetadata;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.config.SingularityTaskMetadataConfiguration;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.history.HistoryManager;
import com.hubspot.singularity.notifications.SingularityIntercom;

@Singleton
public class SingularityNotificationsPoller extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityNotificationsPoller.class);

  private final SingularityConfiguration configuration;
  private final TaskManager taskManager;
  private final RequestManager requestManager;
  private final HistoryManager historyManager;
  private final SingularityIntercom intercom;
  private final SingularityTaskMetadataConfiguration taskMetadataConfiguration;

  @Inject
  SingularityNotificationsPoller(SingularityConfiguration configuration, SingularityTaskMetadataConfiguration taskMetadataConfiguration, TaskManager taskManager, RequestManager requestManager,
                                 HistoryManager historyManager, SingularityIntercom intercom) {
    super(configuration.getCheckQueuedMailsEveryMillis(), TimeUnit.MILLISECONDS);

    this.configuration = configuration;
    this.taskMetadataConfiguration = taskMetadataConfiguration;
    this.taskManager = taskManager;
    this.requestManager = requestManager;
    this.historyManager = historyManager;
    this.intercom = intercom;
  }

  @Override
  protected boolean isEnabled() {
    return configuration.getSmtpConfigurationOptional().isPresent();
  }

  private enum ShouldSendNotificationState {
    SEND, WAIT, ERROR;
  }

  private void checkToSendTaskFinishedMail(SingularityTaskId taskId) {
    Optional<SingularityRequestWithState> requestWithState = requestManager.getRequest(taskId.getRequestId());
    Optional<SingularityTaskHistory> taskHistory = taskManager.getTaskHistory(taskId);

    ShouldSendNotificationState shouldNotifyState = shouldSendTaskFinishedNotification(taskId, requestWithState, taskHistory);

    if (shouldNotifyState == ShouldSendNotificationState.WAIT) {
      return;
    }

    try {
      intercom.sendTaskFinishedNotification(taskHistory.get(), requestWithState.get().getRequest());
    } catch (Throwable t) {
      LOG.error("While trying to send task completed notifications for {}", taskId, t);
    } finally {
      SingularityDeleteResult result = taskManager.deleteFinishedTaskNotificationQueue(taskId);
      LOG.debug("Task {} notifications sent with status {} (delete result {})", taskId, shouldNotifyState, result);
    }
  }

  private ShouldSendNotificationState shouldSendTaskFinishedNotification(SingularityTaskId taskId, Optional<SingularityRequestWithState> requestWithState, Optional<SingularityTaskHistory> taskHistory) {
    if (!requestWithState.isPresent()) {
      LOG.warn("No request found for {}, can't send task finished email", taskId);
      return ShouldSendNotificationState.ERROR;
    }

    if (!taskHistory.isPresent()) {
      taskHistory = historyManager.getTaskHistory(taskId.getId());
    }

    if (!taskHistory.isPresent()) {
      LOG.warn("No task history found for {}, can't send task finished email", taskId);
      return ShouldSendNotificationState.ERROR;
    }

    if (taskMetadataConfiguration.getSendTaskCompletedMailOnceMetadataTypeIsAvailable().isPresent()) {
      for (SingularityTaskMetadata taskMetadata : taskHistory.get().getTaskMetadata()) {
        if (taskMetadata.getType().equals(taskMetadataConfiguration.getSendTaskCompletedMailOnceMetadataTypeIsAvailable().get())) {
          LOG.debug("Sending task finished email for {} because metadata type {} is present", taskId,
              taskMetadataConfiguration.getSendTaskCompletedMailOnceMetadataTypeIsAvailable().get());
          return ShouldSendNotificationState.SEND;
        }
      }
    }

    // check to see if it's too soon.
    if (taskMetadataConfiguration.getWaitToSendTaskCompletedMailBufferMillis() > 0) {
      Optional<SingularityTaskHistoryUpdate> lastUpdate = taskHistory.get().getLastTaskUpdate();

      if (!lastUpdate.isPresent()) {
        LOG.warn("Missing last update for {}, can't send task finished email", taskId);
        return ShouldSendNotificationState.ERROR;
      }

      final long timeSinceLastUpdate = System.currentTimeMillis() - lastUpdate.get().getTimestamp();

      if (timeSinceLastUpdate < taskMetadataConfiguration.getWaitToSendTaskCompletedMailBufferMillis()) {
        LOG.debug("Not sending task finished email for {} because last update was {} ago, buffer is {}", taskId, JavaUtils.durationFromMillis(timeSinceLastUpdate),
            JavaUtils.durationFromMillis(taskMetadataConfiguration.getWaitToSendTaskCompletedMailBufferMillis()));
        return ShouldSendNotificationState.WAIT;
      }
    }

    return ShouldSendNotificationState.SEND;
  }

  @Override
  public void runActionOnPoll() {
    for (SingularityTaskId finishedTaskId : taskManager.getTaskFinishedNotificationQueue()) {
      checkToSendTaskFinishedMail(finishedTaskId);
    }
  }

  @Override
  protected boolean abortsOnError() {
    return false;
  }

}
