package com.hubspot.singularity.event;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.hubspot.singularity.config.WebhookQueueConfiguration;
import com.hubspot.singularity.data.ZkWebhookQueue;
import com.hubspot.singularity.hooks.SnsWebhookQueue;
import com.hubspot.singularity.hooks.WebhookQueueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingularityEventModule implements Module {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityEventModule.class);

  private final WebhookQueueConfiguration webhookQueueConfiguration;

  public SingularityEventModule(WebhookQueueConfiguration webhookQueueConfiguration) {
    this.webhookQueueConfiguration = webhookQueueConfiguration;
  }

  @Override
  public void configure(final Binder binder) {
    Multibinder<SingularityEventSender> eventListeners = Multibinder.newSetBinder(
      binder,
      SingularityEventSender.class
    );
    if (webhookQueueConfiguration.getQueueType() == WebhookQueueType.SNS) {
      LOG.info("Binding sns webhook managed");
      eventListeners.addBinding().to(SnsWebhookQueue.class).in(Scopes.SINGLETON);
    } else {
      LOG.info("Binding zookeeper webhook manager");
      eventListeners.addBinding().to(ZkWebhookQueue.class).in(Scopes.SINGLETON);
    }
    binder
      .bind(SingularityEventListener.class)
      .to(SingularityEventController.class)
      .in(Scopes.SINGLETON);
  }
}
