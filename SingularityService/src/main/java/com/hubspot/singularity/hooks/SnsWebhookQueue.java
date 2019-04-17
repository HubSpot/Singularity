package com.hubspot.singularity.hooks;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityDeployUpdate;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskWebhook;
import com.hubspot.singularity.event.SingularityEventSender;

@Singleton
public class SnsWebhookQueue implements SingularityEventSender {
  private final SnsWebhookManager snsWebhookManager;

  @Inject
  public SnsWebhookQueue(SnsWebhookManager snsWebhookManager) {
    this.snsWebhookManager = snsWebhookManager;
  }

  @Override
  public void requestHistoryEvent(SingularityRequestHistory requestUpdate) {
    snsWebhookManager.requestHistoryEvent(requestUpdate);
  }

  @Override
  public void taskWebhookEvent(SingularityTaskWebhook taskWebhook) {
    snsWebhookManager.taskWebhook(taskWebhook);
  }

  @Override
  public void deployHistoryEvent(SingularityDeployUpdate deployUpdate) {
    snsWebhookManager.deployHistoryEvent(deployUpdate);
  }
}
