package com.hubspot.singularity.hooks;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityDeployUpdate;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.data.WebhookManager;

@Singleton
public class SnsWebhookRetryer extends AbstractWebhookChecker {

  private final SnsWebhookManager snsWebhookManager;
  private final WebhookManager webhookManager;

  @Inject
  public SnsWebhookRetryer(SnsWebhookManager snsWebhookManager,
                           WebhookManager webhookManager) {
    this.snsWebhookManager = snsWebhookManager;
    this.webhookManager = webhookManager;
  }


  public void checkWebhooks() {
    for (SingularityTaskHistoryUpdate taskHistoryUpdate : webhookManager.getTaskUpdatesToRetry()) {
      snsWebhookManager.taskHistoryUpdateEvent(taskHistoryUpdate);
    }
    for (SingularityDeployUpdate deployUpdate : webhookManager.getDeployUpdatesToRetry()) {
      snsWebhookManager.deployHistoryEvent(deployUpdate);
    }
    for (SingularityRequestHistory requestHistory : webhookManager.getRequestUpdatesToRetry()) {
      snsWebhookManager.requestHistoryEvent(requestHistory);
    }
  }
}
