package com.hubspot.singularity.hooks;

import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityWebhook;
import com.hubspot.singularity.data.WebhookManager;

public class SingularityRequestWebhookAsyncHandler extends AbstractSingularityWebhookAsyncHandler<SingularityRequestHistory>  {
  
  private final WebhookManager webhookManager;

  public SingularityRequestWebhookAsyncHandler(WebhookManager webhookManager, SingularityWebhook webhook, SingularityRequestHistory requestUpdate, boolean shouldDeleteUpdateDueToQueueAboveCapacity) {
    super(webhook, requestUpdate, shouldDeleteUpdateDueToQueueAboveCapacity);
    
    this.webhookManager = webhookManager;
  }

  @Override
  public void deleteWebhookUpdate() {
    webhookManager.deleteRequestUpdate(webhook, update);    
  }
  
}
