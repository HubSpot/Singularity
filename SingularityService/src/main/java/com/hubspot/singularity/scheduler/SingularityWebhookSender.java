package com.hubspot.singularity.scheduler;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityCloseable;
import com.hubspot.singularity.SingularityCloser;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityWebhook;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.WebhookManager;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;
import com.ning.http.client.AsyncHttpClient;

public class SingularityWebhookSender implements SingularityCloseable {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityWebhookSender.class);

  private final AsyncHttpClient http;
  private final WebhookManager webhookManager;
  private final SingularityExceptionNotifier exceptionNotifier;
  private final SingularityConfiguration configuration;
  private final SingularityCloser closer;

  @Inject
  public SingularityWebhookSender(AsyncHttpClient http, SingularityCloser closer, SingularityExceptionNotifier exceptionNotifier, WebhookManager webhookManager, SingularityConfiguration configuration) {
    this.http = http;
    this.closer = closer;
    this.webhookManager = webhookManager;
    this.configuration = configuration;
    this.exceptionNotifier = exceptionNotifier;
  }

  public void checkWebhooks() {
    final long start = System.currentTimeMillis();

    final List<SingularityWebhook> webhooks = webhookManager.getActiveWebhooks();
    if (webhooks.isEmpty()) {
      return;
    }

    int numUpdates = 0;

    for (SingularityWebhook webhook : webhooks) {
      List<SingularityTaskHistoryUpdate> taskUpdates = webhookManager.getQueuedUpdatesForHook(webhook.getId());

      // check for overage
      if (taskUpdates.size() > configuration.getMaxQueuedUpdatesPerWebhook()) {
        // discard the oldest.
      }

    }

    LOG.info("Sent {} updates for {} webhooks in {}", numUpdates, webhooks.size(), JavaUtils.duration(start));
  }

  public void close() {
    // closer.shutdown(getClass().getName(), executorService, 1);
  }

}
