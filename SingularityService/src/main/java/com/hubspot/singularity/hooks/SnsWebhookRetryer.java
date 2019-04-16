package com.hubspot.singularity.hooks;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityDeployUpdate;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.data.WebhookManager;

@Singleton
public class SnsWebhookRetryer extends AbstractWebhookChecker {
  private final SnsWebhookQueue webhookQueue;
  private final WebhookManager webhookManager;

  @Inject
  public SnsWebhookRetryer(SnsWebhookQueue webhookQueue,
                           WebhookManager webhookManager) {
    this.webhookQueue = webhookQueue;
    this.webhookManager = webhookManager;
  }

  public void checkWebhooks() {
    for (SingularityTaskHistoryUpdate taskHistoryUpdate : webhookManager.getTaskUpdatesToRetry()) {
      webhookQueue.taskHistoryUpdateEvent(taskHistoryUpdate);
    }
    for (SingularityDeployUpdate deployUpdate : webhookManager.getDeployUpdatesToRetry()) {
      webhookQueue.deployHistoryEvent(deployUpdate);
    }
    for (SingularityRequestHistory requestHistory : webhookManager.getRequestUpdatesToRetry()) {
      webhookQueue.requestHistoryEvent(requestHistory);
    }
  }
}
