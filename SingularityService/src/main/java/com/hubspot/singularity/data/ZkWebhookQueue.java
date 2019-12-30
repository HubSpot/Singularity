package com.hubspot.singularity.data;

import com.google.inject.Inject;
import com.hubspot.singularity.CrashLoopInfo;
import com.hubspot.singularity.SingularityDeployUpdate;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskWebhook;
import com.hubspot.singularity.event.SingularityEventSender;

public class ZkWebhookQueue implements SingularityEventSender {
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
  public void taskWebhookEvent(SingularityTaskWebhook taskWebhook) {
    webhookManager.saveTaskHistoryUpdateEvent(taskWebhook.getTaskUpdate());
  }

  @Override
  public void deployHistoryEvent(SingularityDeployUpdate deployUpdate) {
    webhookManager.saveDeployHistoryEvent(deployUpdate);
  }

  @Override
  public void crashLoopEvent(CrashLoopInfo crashLoopUpdate) {
    webhookManager.saveCrashLoopEvent(crashLoopUpdate);
  }
}
