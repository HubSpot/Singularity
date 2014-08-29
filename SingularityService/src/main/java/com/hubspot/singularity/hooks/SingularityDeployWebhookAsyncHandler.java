package com.hubspot.singularity.hooks;

import com.hubspot.singularity.SingularityDeployWebhook;
import com.hubspot.singularity.SingularityWebhook;
import com.hubspot.singularity.data.WebhookManager;

public class SingularityDeployWebhookAsyncHandler extends AbstractSingularityWebhookAsyncHandler<SingularityDeployWebhook>  {
  
  private final WebhookManager webhookManager;

  public SingularityDeployWebhookAsyncHandler(WebhookManager webhookManager, SingularityWebhook webhook, SingularityDeployWebhook deployUpdate, boolean shouldDeleteUpdateDueToQueueAboveCapacity) {
    super(webhook, deployUpdate, shouldDeleteUpdateDueToQueueAboveCapacity);
    
    this.webhookManager = webhookManager;
  }

  @Override
  public void deleteWebhookUpdate() {
    webhookManager.deleteDeployUpdate(webhook, update);    
  }
  
}
