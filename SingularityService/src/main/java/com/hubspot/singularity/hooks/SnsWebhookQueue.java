package com.hubspot.singularity.hooks;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityDeployUpdate;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.event.SingularityEventListener;

@Singleton
public class SnsWebhookQueue implements SingularityEventListener {
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
  public void taskHistoryUpdateEvent(SingularityTaskHistoryUpdate taskUpdate) {
    snsWebhookManager.taskHistoryUpdateEvent(taskUpdate);
  }

  @Override
  public void deployHistoryEvent(SingularityDeployUpdate deployUpdate) {
    snsWebhookManager.deployHistoryEvent(deployUpdate);
  }
}
