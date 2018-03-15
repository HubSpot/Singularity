package com.hubspot.singularity.hooks;

import com.hubspot.singularity.api.deploy.SingularityDeployUpdate;
import com.hubspot.singularity.api.webhooks.SingularityWebhook;
import com.hubspot.singularity.data.WebhookManager;

public class SingularityDeployWebhookAsyncHandler extends AbstractSingularityWebhookAsyncHandler<SingularityDeployUpdate>  {

  private final WebhookManager webhookManager;

  public SingularityDeployWebhookAsyncHandler(WebhookManager webhookManager, SingularityWebhook webhook, SingularityDeployUpdate deployUpdate, boolean shouldDeleteUpdateDueToQueueAboveCapacity) {
    super(webhook, deployUpdate, shouldDeleteUpdateDueToQueueAboveCapacity);

    this.webhookManager = webhookManager;
  }

  @Override
  public void deleteWebhookUpdate() {
    webhookManager.deleteDeployUpdate(webhook, update);
  }

}
