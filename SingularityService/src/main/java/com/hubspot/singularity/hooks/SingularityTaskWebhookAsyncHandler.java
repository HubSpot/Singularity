package com.hubspot.singularity.hooks;

import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityWebhook;
import com.hubspot.singularity.data.WebhookManager;

public class SingularityTaskWebhookAsyncHandler extends AbstractSingularityWebhookAsyncHandler<SingularityTaskHistoryUpdate>  {

  private final WebhookManager webhookManager;

  public SingularityTaskWebhookAsyncHandler(WebhookManager webhookManager, SingularityWebhook webhook, SingularityTaskHistoryUpdate taskUpdate, boolean shouldDeleteUpdateDueToQueueAboveCapacity) {
    super(webhook, taskUpdate, shouldDeleteUpdateDueToQueueAboveCapacity);

    this.webhookManager = webhookManager;
  }

  @Override
  public void deleteWebhookUpdate() {
    webhookManager.deleteTaskUpdate(webhook, update);
  }

}
