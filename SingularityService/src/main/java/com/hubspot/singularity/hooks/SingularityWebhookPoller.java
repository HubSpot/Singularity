package com.hubspot.singularity.hooks;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import com.google.inject.Inject;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.scheduler.SingularityLeaderOnlyPoller;

@Singleton
public class SingularityWebhookPoller extends SingularityLeaderOnlyPoller {

  private final SingularityWebhookSender webhookSender;

  @Inject
  public SingularityWebhookPoller(SingularityWebhookSender webhookSender, SingularityConfiguration configuration) {
    super(configuration.getCheckWebhooksEveryMillis(), TimeUnit.MILLISECONDS);

    this.webhookSender = webhookSender;
  }

  @Override
  public void runActionOnPoll() {
    webhookSender.checkWebhooks();
  }

  @Override
  protected boolean abortsOnError() {
    return false;
  }


}
