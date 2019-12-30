package com.hubspot.singularity.hooks;

import java.util.Optional;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.CrashLoopInfo;
import com.hubspot.singularity.SingularityDeployUpdate;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskWebhook;
import com.hubspot.singularity.data.WebhookManager;
import com.hubspot.singularity.data.history.TaskHistoryHelper;

@Singleton
public class SnsWebhookRetryer extends AbstractWebhookChecker {

  private final SnsWebhookManager snsWebhookManager;
  private final WebhookManager webhookManager;
  private final TaskHistoryHelper taskHistoryHelper;

  @Inject
  public SnsWebhookRetryer(SnsWebhookManager snsWebhookManager,
                           WebhookManager webhookManager,
                           TaskHistoryHelper taskHistoryHelper) {
    this.snsWebhookManager = snsWebhookManager;
    this.webhookManager = webhookManager;
    this.taskHistoryHelper = taskHistoryHelper;
  }


  public void checkWebhooks() {
    for (SingularityTaskHistoryUpdate taskHistoryUpdate : webhookManager.getTaskUpdatesToRetry()) {
      Optional<SingularityTask> task = taskHistoryHelper.getTask(taskHistoryUpdate.getTaskId());
      if (task.isPresent()) {
        snsWebhookManager.taskWebhook(new SingularityTaskWebhook(task.get(), taskHistoryUpdate));
      }
      webhookManager.deleteTaskUpdateForRetry(taskHistoryUpdate);
    }
    for (SingularityDeployUpdate deployUpdate : webhookManager.getDeployUpdatesToRetry()) {
      snsWebhookManager.deployHistoryEvent(deployUpdate);
      webhookManager.deleteDeployUpdateForRetry(deployUpdate);
    }
    for (SingularityRequestHistory requestHistory : webhookManager.getRequestUpdatesToRetry()) {
      snsWebhookManager.requestHistoryEvent(requestHistory);
      webhookManager.deleteRequestUpdateForRetry(requestHistory);
    }
    for (CrashLoopInfo crashLoopUpdate : webhookManager.getCrashLoopUpdatesToRetry()) {
      snsWebhookManager.crashLoopEvent(crashLoopUpdate);
      webhookManager.deleteCrashLoopUpdateForRetry(crashLoopUpdate);
    }
  }
}
