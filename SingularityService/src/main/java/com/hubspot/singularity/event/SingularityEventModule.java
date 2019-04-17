package com.hubspot.singularity.event;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.hubspot.singularity.config.WebhookQueueConfiguration;
import com.hubspot.singularity.hooks.SnsWebhookQueue;
import com.hubspot.singularity.data.ZkWebhookQueue;
import com.hubspot.singularity.hooks.WebhookQueueType;

public class SingularityEventModule implements Module {
  private final WebhookQueueConfiguration webhookQueueConfiguration;

  public SingularityEventModule(WebhookQueueConfiguration webhookQueueConfiguration) {
    this.webhookQueueConfiguration = webhookQueueConfiguration;
  }

  @Override
  public void configure(final Binder binder) {
    Multibinder<SingularityEventListener> eventListeners = Multibinder.newSetBinder(binder, SingularityEventListener.class);
    if (webhookQueueConfiguration.getQueueType() == WebhookQueueType.SNS) {
      eventListeners.addBinding().to(SnsWebhookQueue.class).in(Scopes.SINGLETON);
    } else {
      eventListeners.addBinding().to(ZkWebhookQueue.class).in(Scopes.SINGLETON);
    }
    binder.bind(SingularityEventListener.class).to(SingularityEventController.class).in(Scopes.SINGLETON);
  }
}
