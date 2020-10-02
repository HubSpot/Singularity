package com.hubspot.singularity.hooks;

import com.hubspot.singularity.ElevatedAccessEvent;
import com.hubspot.singularity.SingularityWebhook;
import com.hubspot.singularity.data.WebhookManager;

public class SingularityElevatedAccessEventAsyncHandler
  extends AbstractSingularityWebhookAsyncHandler<ElevatedAccessEvent> {
  private final WebhookManager webhookManager;

  public SingularityElevatedAccessEventAsyncHandler(
    WebhookManager webhookManager,
    SingularityWebhook webhook,
    ElevatedAccessEvent accessEvent,
    boolean shouldDeleteUpdateDueToQueueAboveCapacity
  ) {
    super(webhook, accessEvent, shouldDeleteUpdateDueToQueueAboveCapacity);
    this.webhookManager = webhookManager;
  }

  @Override
  public void deleteWebhookUpdate() {
    webhookManager.deleteElevatedAccessEvent(webhook, update);
  }
}
