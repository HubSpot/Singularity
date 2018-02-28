package com.hubspot.singularity.hooks;

import com.hubspot.singularity.api.request.SingularityRequestHistory;
import com.hubspot.singularity.api.webhooks.SingularityWebhook;
import com.hubspot.singularity.data.WebhookManager;

public class SingularityRequestWebhookAsyncHandler extends AbstractSingularityWebhookAsyncHandler<SingularityRequestHistory>  {

  private final WebhookManager webhookManager;

  public SingularityRequestWebhookAsyncHandler(WebhookManager webhookManager, SingularityWebhook webhook, SingularityRequestHistory requestUpdate, boolean shouldDeleteUpdateOnFailure) {
    super(webhook, requestUpdate, shouldDeleteUpdateOnFailure);

    this.webhookManager = webhookManager;
  }

  @Override
  public void deleteWebhookUpdate() {
    webhookManager.deleteRequestUpdate(webhook, update);
  }

}
