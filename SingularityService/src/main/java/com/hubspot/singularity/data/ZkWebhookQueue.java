package com.hubspot.singularity.data;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityDeployUpdate;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.event.SingularityEventListener;

public class ZkWebhookQueue implements SingularityEventListener {
  private final WebhookManager webhookManager;

  @Inject
  public ZkWebhookQueue(WebhookManager webhookManager) {
    this.webhookManager = webhookManager;
  }

  @Override
  public void requestHistoryEvent(SingularityRequestHistory requestUpdate) {
    webhookManager.saveRequestHistoryEvent(requestUpdate);
  }

  @Override
  public void taskHistoryUpdateEvent(SingularityTaskHistoryUpdate taskUpdate) {
    webhookManager.saveTaskHistoryUpdateEvent(taskUpdate);
  }

  @Override
  public void deployHistoryEvent(SingularityDeployUpdate deployUpdate) {
    webhookManager.saveDeployHistoryEvent(deployUpdate);
  }
}
