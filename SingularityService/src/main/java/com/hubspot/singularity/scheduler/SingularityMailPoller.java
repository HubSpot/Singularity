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
import com.hubspot.singularity.config.SingularityConfiguration;
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

  @Inject
  SingularityMailPoller(SingularityConfiguration configuration, TaskManager taskManager, RequestManager requestManager, HistoryManager historyManager, SingularityMailer mailer) {
    super(configuration.getCleanupEverySeconds(), TimeUnit.SECONDS);

    this.configuration = configuration;
    this.taskManager = taskManager;
    this.requestManager = requestManager;
    this.historyManager = historyManager;
    this.mailer = mailer;
  }

  @Override
  protected boolean isEnabled() {
    return configuration.getSmtpConfiguration().isPresent();
  }

  private enum CheckToSendTaskFinishedMailState {
    SENT, WAITING, ERROR;
  }

  private CheckToSendTaskFinishedMailState checkToSendTaskFinishedMail(SingularityTaskId taskId) {
    Optional<SingularityRequestWithState> requestWithState = requestManager.getRequest(taskId.getRequestId());

    if (!requestWithState.isPresent()) {
      LOG.warn("No request found for {}, can't send task finished email", taskId);
      return CheckToSendTaskFinishedMailState.ERROR;
    }

    Optional<SingularityTaskHistory> taskHistory = taskManager.getTaskHistory(taskId);

    if (!taskHistory.isPresent()) {
      taskHistory = historyManager.getTaskHistory(taskId.getId());
    }

    if (!taskHistory.isPresent()) {
      LOG.warn("No task history found for {}, can't send task finished email", taskId);
      return CheckToSendTaskFinishedMailState.ERROR;
    }

    // check to see if it's too soon.
    if (configuration.getWaitToSendTaskCompletedMailBufferMillis() > 0) {
      Optional<SingularityTaskHistoryUpdate> lastUpdate = taskHistory.get().getLastTaskUpdate();

      if (!lastUpdate.isPresent()) {
        LOG.warn("Missing last update for {}, can't send task finished email", taskId);
        return CheckToSendTaskFinishedMailState.ERROR;
      }

      final long timeSinceLastUpdate = System.currentTimeMillis() - lastUpdate.get().getTimestamp();

      if (timeSinceLastUpdate < configuration.getWaitToSendTaskCompletedMailBufferMillis()) {
        LOG.debug("Not sending task finished for {} email because last update was {} ago, buffer is {}", taskId, JavaUtils.durationFromMillis(timeSinceLastUpdate),
            JavaUtils.durationFromMillis(configuration.getWaitToSendTaskCompletedMailBufferMillis()));
        return CheckToSendTaskFinishedMailState.WAITING;
      }
    }

    try {
      mailer.sendTaskCompletedMail(taskHistory.get(), requestWithState.get().getRequest());
      return CheckToSendTaskFinishedMailState.SENT;
    } catch (Throwable t) {
      LOG.error("While trying to send task completed mail for {}", taskId, t);
      return CheckToSendTaskFinishedMailState.ERROR;
    }
  }

  @Override
  public void runActionOnPoll() {
    for (SingularityTaskId finishedTaskId : taskManager.getTaskFinishedMailQueue()) {
      CheckToSendTaskFinishedMailState mailSendState = checkToSendTaskFinishedMail(finishedTaskId);

      switch (mailSendState) {
        case SENT:
        case ERROR:
          SingularityDeleteResult result = taskManager.deleteFinishedTaskMailQueue(finishedTaskId);
          LOG.debug("Task {} mail sent with status {} (delete result {})", finishedTaskId, mailSendState, result);
          break;
        case WAITING:
          break;
      }
    }
  }

  @Override
  protected boolean abortsOnError() {
    return false;
  }

}
