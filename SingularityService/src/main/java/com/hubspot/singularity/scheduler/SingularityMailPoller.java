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
import com.hubspot.singularity.smtp.SingularityMailer;

@Singleton
public class SingularityMailPoller extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityMailPoller.class);

  private final SingularityConfiguration configuration;
  private final TaskManager taskManager;
  private final RequestManager requestManager;
  private final HistoryManager historyManager;
  private final SingularityMailer mailer;
  private final SingularityTaskMetadataConfiguration taskMetadataConfiguration;

  @Inject
  SingularityMailPoller(SingularityConfiguration configuration, SingularityTaskMetadataConfiguration taskMetadataConfiguration, TaskManager taskManager, RequestManager requestManager,
      HistoryManager historyManager, SingularityMailer mailer) {
    super(configuration.getCheckQueuedMailsEveryMillis(), TimeUnit.MILLISECONDS);

    this.configuration = configuration;
    this.taskMetadataConfiguration = taskMetadataConfiguration;
    this.taskManager = taskManager;
    this.requestManager = requestManager;
    this.historyManager = historyManager;
    this.mailer = mailer;
  }

  @Override
  protected boolean isEnabled() {
    return configuration.getSmtpConfiguration().isPresent();
  }

  private enum ShouldSendMailState {
    SEND, WAIT, ERROR;
  }

  private void checkToSendTaskFinishedMail(SingularityTaskId taskId) {
    Optional<SingularityRequestWithState> requestWithState = requestManager.getRequest(taskId.getRequestId());
    Optional<SingularityTaskHistory> taskHistory = taskManager.getTaskHistory(taskId);

    ShouldSendMailState shouldSendState = shouldSendTaskFinishedMail(taskId, requestWithState, taskHistory);

    if (shouldSendState == ShouldSendMailState.WAIT) {
      return;
    }

    try {
      mailer.sendTaskCompletedMail(taskHistory.get(), requestWithState.get().getRequest());
    } catch (Throwable t) {
      LOG.error("While trying to send task completed mail for {}", taskId, t);
    } finally {
      SingularityDeleteResult result = taskManager.deleteFinishedTaskMailQueue(taskId);
      LOG.debug("Task {} mail sent with status {} (delete result {})", taskId, shouldSendState, result);
    }
  }

  private ShouldSendMailState shouldSendTaskFinishedMail(SingularityTaskId taskId, Optional<SingularityRequestWithState> requestWithState, Optional<SingularityTaskHistory> taskHistory) {
    if (!requestWithState.isPresent()) {
      LOG.warn("No request found for {}, can't send task finished email", taskId);
      return ShouldSendMailState.ERROR;
    }

    if (!taskHistory.isPresent()) {
      taskHistory = historyManager.getTaskHistory(taskId.getId());
    }

    if (!taskHistory.isPresent()) {
      LOG.warn("No task history found for {}, can't send task finished email", taskId);
      return ShouldSendMailState.ERROR;
    }

    if (taskMetadataConfiguration.getSendTaskCompletedMailOnceMetadataTypeIsAvailable().isPresent()) {
      for (SingularityTaskMetadata taskMetadata : taskHistory.get().getTaskMetadata()) {
        if (taskMetadata.getType().equals(taskMetadataConfiguration.getSendTaskCompletedMailOnceMetadataTypeIsAvailable().get())) {
          LOG.debug("Sending task finished email for {} because metadata type {} is present", taskId,
              taskMetadataConfiguration.getSendTaskCompletedMailOnceMetadataTypeIsAvailable().get());
          return ShouldSendMailState.SEND;
        }
      }
    }

    // check to see if it's too soon.
    if (taskMetadataConfiguration.getWaitToSendTaskCompletedMailBufferMillis() > 0) {
      Optional<SingularityTaskHistoryUpdate> lastUpdate = taskHistory.get().getLastTaskUpdate();

      if (!lastUpdate.isPresent()) {
        LOG.warn("Missing last update for {}, can't send task finished email", taskId);
        return ShouldSendMailState.ERROR;
      }

      final long timeSinceLastUpdate = System.currentTimeMillis() - lastUpdate.get().getTimestamp();

      if (timeSinceLastUpdate < taskMetadataConfiguration.getWaitToSendTaskCompletedMailBufferMillis()) {
        LOG.debug("Not sending task finished email for {} because last update was {} ago, buffer is {}", taskId, JavaUtils.durationFromMillis(timeSinceLastUpdate),
            JavaUtils.durationFromMillis(taskMetadataConfiguration.getWaitToSendTaskCompletedMailBufferMillis()));
        return ShouldSendMailState.WAIT;
      }
    }

    return ShouldSendMailState.SEND;
  }

  @Override
  public void runActionOnPoll() {
    for (SingularityTaskId finishedTaskId : taskManager.getTaskFinishedMailQueue()) {
      checkToSendTaskFinishedMail(finishedTaskId);
    }
  }

  @Override
  protected boolean abortsOnError() {
    return false;
  }

}
