package com.hubspot.singularity.hooks;

import com.google.inject.Inject;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.scheduler.SingularityLeaderOnlyPoller;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;

@Singleton
public class SingularityWebhookPoller extends SingularityLeaderOnlyPoller {
  private final AbstractWebhookChecker webhookSender;

  @Inject
  public SingularityWebhookPoller(
    AbstractWebhookChecker webhookSender,
    SingularityConfiguration configuration
  ) {
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
