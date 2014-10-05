package com.hubspot.singularity.hooks;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityCloseable;
import com.hubspot.singularity.SingularityCloser;
import com.hubspot.singularity.SingularityStartable;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

public class SingularityWebhookPoller extends SingularityCloseable<ScheduledExecutorService> implements SingularityStartable {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityWebhookPoller.class);

  private final SingularityWebhookSender webhookSender;
  private final SingularityExceptionNotifier exceptionNotifier;
  private final SingularityConfiguration configuration;
  private final ScheduledExecutorService executorService;

  @Inject
  public SingularityWebhookPoller(SingularityWebhookSender webhookSender, SingularityCloser closer, SingularityExceptionNotifier exceptionNotifier, SingularityConfiguration configuration) {
    super(closer);

    this.webhookSender = webhookSender;
    this.configuration = configuration;
    this.exceptionNotifier = exceptionNotifier;

    this.executorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("SingularityWebhookSender-%d").build());
  }

  @Override
  public void start() {
    LOG.info("Starting a webhookPoller that executes webhooks every {}", JavaUtils.durationFromMillis(configuration.getCheckWebhooksEveryMillis()));

    executorService.scheduleAtFixedRate(new Runnable() {

      @Override
      public void run() {
        try {
          webhookSender.checkWebhooks();
        } catch (Throwable t) {
          LOG.error("Caught an unexpected exception while checking webhooks", t);
          exceptionNotifier.notify(t);
        }
      }
    }, configuration.getCheckWebhooksEveryMillis(), configuration.getCheckWebhooksEveryMillis(), TimeUnit.MILLISECONDS);
  }

  @Override
  public Optional<ScheduledExecutorService> getExecutorService() {
    return Optional.of(executorService);
  }

}
