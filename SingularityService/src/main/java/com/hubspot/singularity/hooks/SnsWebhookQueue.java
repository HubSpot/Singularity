package com.hubspot.singularity.hooks;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityDeployUpdate;
import com.hubspot.singularity.SingularityManagedCachedThreadPoolFactory;
import com.hubspot.singularity.SingularityManagedScheduledExecutorServiceFactory;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.WebhookType;
import com.hubspot.singularity.async.AsyncSemaphore;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.config.WebhookQueueConfiguration;
import com.hubspot.singularity.event.SingularityEventListener;

@Singleton
public class SnsWebhookQueue implements SingularityEventListener {
  private static final Logger LOG = LoggerFactory.getLogger(SnsWebhookQueue.class);

  private final WebhookQueueConfiguration webhookConf;
  private final ObjectMapper objectMapper;
  private final AmazonSNS snsClient;
  private final AsyncSemaphore<PublishResult> publishSemaphore;
  private final ExecutorService publishExecutor;

  private final Map<WebhookType, String> typeToArn;

  @Inject
  public SnsWebhookQueue(ObjectMapper objectMapper,
                         SingularityConfiguration configuration,
                         SingularityManagedScheduledExecutorServiceFactory executorServiceFactory,
                         SingularityManagedCachedThreadPoolFactory managedCachedThreadPoolFactory) {
    this.objectMapper = objectMapper;
    this.webhookConf = configuration.getWebhookQueueConfiguration();
    if (webhookConf.getAwsAccessKey().isPresent() && webhookConf.getAwsSecretKey().isPresent()) {
      this.snsClient = AmazonSNSClient.builder()
          .withCredentials(new AWSStaticCredentialsProvider(
              new BasicAWSCredentials(webhookConf.getAwsAccessKey().get(), webhookConf.getAwsSecretKey().get())
          )).withClientConfiguration(
              new ClientConfiguration()
              .withMaxConnections(configuration.getMaxConcurrentWebhooks())
          )
          .build();
    } else {
      this.snsClient = AmazonSNSClientBuilder.defaultClient();
    }
    this.publishSemaphore = AsyncSemaphore.newBuilder(configuration::getMaxConcurrentWebhooks, executorServiceFactory.get("webhook-publish-semaphore", 1)).build();
    this.publishExecutor = managedCachedThreadPoolFactory.get("webhook-publish");
    this.typeToArn = new ConcurrentHashMap<>();
  }

  @Override
  public void requestHistoryEvent(SingularityRequestHistory requestUpdate) {
    publish(WebhookType.REQUEST, requestUpdate)
        .whenComplete((result, throwable) -> {
          if (throwable != null) {
            LOG.error("Could not submit update {}", requestUpdate, throwable);
          } else if (result != null) {
            LOG.trace("Sent update {} with messageId {}", requestUpdate, result.getMessageId());
          }
        });
  }

  @Override
  public void taskHistoryUpdateEvent(SingularityTaskHistoryUpdate taskUpdate) {
    publish(WebhookType.TASK, taskUpdate)
        .whenComplete((result, throwable) -> {
          if (throwable != null) {
            LOG.error("Could not submit update {}", taskUpdate, throwable);
          } else if (result != null) {
            LOG.trace("Sent update {} with messageId {}", taskUpdate, result.getMessageId());
          }
        });
  }

  @Override
  public void deployHistoryEvent(SingularityDeployUpdate deployUpdate) {
    publish(WebhookType.DEPLOY, deployUpdate)
      .whenComplete((result, throwable) -> {
        if (throwable != null) {
          LOG.error("Could not submit update {}", deployUpdate, throwable);
        } else if (result != null) {
          LOG.trace("Sent update {} with messageId {}", deployUpdate, result.getMessageId());
        }
      });
  }

  private String getOrCreateSnsTopic(WebhookType type) {
    return typeToArn.computeIfAbsent(type, (t) -> {
      CreateTopicRequest createTopicRequest = new CreateTopicRequest(webhookConf.getSnsTopics().get(type));
      CreateTopicResult createTopicResult = snsClient.createTopic(createTopicRequest);
      return createTopicResult.getTopicArn();
    });
  }

  private <T> CompletableFuture<PublishResult> publish(WebhookType type, T content) {
    try {
      return publishSemaphore.call(() ->
        CompletableFuture.supplyAsync(() -> {
          try {
            PublishRequest publishRequest = new PublishRequest(
                getOrCreateSnsTopic(type),
                objectMapper.writeValueAsString(content)
            );
            return snsClient.publish(publishRequest);
          } catch (IOException ioe) {
            throw new RuntimeException(ioe);
          }
        }, publishExecutor));
    } catch (Throwable t) {
      LOG.error("Could not submit webhook for publish, dropping", t);
      return CompletableFuture.completedFuture(null);
    }
  }
}
