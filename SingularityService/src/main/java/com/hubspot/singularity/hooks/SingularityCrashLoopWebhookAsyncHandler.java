package com.hubspot.singularity.hooks;

import com.hubspot.singularity.CrashLoopInfo;
import com.hubspot.singularity.SingularityWebhook;
import com.hubspot.singularity.data.WebhookManager;

public class SingularityCrashLoopWebhookAsyncHandler
  extends AbstractSingularityWebhookAsyncHandler<CrashLoopInfo> {

  private final WebhookManager webhookManager;

  public SingularityCrashLoopWebhookAsyncHandler(
    WebhookManager webhookManager,
    SingularityWebhook webhook,
    CrashLoopInfo crashLoopUpdate,
    boolean shouldDeleteUpdateDueToQueueAboveCapacity
  ) {
    super(webhook, crashLoopUpdate, shouldDeleteUpdateDueToQueueAboveCapacity);
    this.webhookManager = webhookManager;
  }

  @Override
  public void deleteWebhookUpdate() {
    webhookManager.deleteCrashLoopUpdate(webhook, update);
  }
}
