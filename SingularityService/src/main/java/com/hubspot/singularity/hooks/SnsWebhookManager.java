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
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityDeployUpdate;
import com.hubspot.singularity.SingularityManagedCachedThreadPoolFactory;
import com.hubspot.singularity.SingularityManagedScheduledExecutorServiceFactory;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskWebhook;
import com.hubspot.singularity.WebhookType;
import com.hubspot.singularity.async.AsyncSemaphore;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.config.WebhookQueueConfiguration;
import com.hubspot.singularity.data.WebhookManager;
import com.hubspot.singularity.data.history.TaskHistoryHelper;

@Singleton
public class SnsWebhookManager {
  private static final Logger LOG = LoggerFactory.getLogger(SnsWebhookManager.class);

  private final WebhookQueueConfiguration webhookConf;
  private final ObjectMapper objectMapper;
  private final AmazonSNS snsClient;
  private final AsyncSemaphore<Void> publishSemaphore;
  private final ExecutorService publishExecutor;
  private final WebhookManager webhookManager;
  private final TaskHistoryHelper taskHistoryHelper;

  private final Map<WebhookType, String> typeToArn;

  @Inject
  public SnsWebhookManager(ObjectMapper objectMapper,
                           SingularityConfiguration configuration,
                           SingularityManagedScheduledExecutorServiceFactory executorServiceFactory,
                           SingularityManagedCachedThreadPoolFactory managedCachedThreadPoolFactory,
                           TaskHistoryHelper taskHistoryHelper,
                           WebhookManager webhookManager) {
    this.objectMapper = objectMapper;
    this.webhookConf = configuration.getWebhookQueueConfiguration();
    this.taskHistoryHelper = taskHistoryHelper;
    if (webhookConf.getAwsAccessKey().isPresent() && webhookConf.getAwsSecretKey().isPresent()) {
      this.snsClient = AmazonSNSClient.builder()
          .withCredentials(new AWSStaticCredentialsProvider(
              new BasicAWSCredentials(webhookConf.getAwsAccessKey().get(), webhookConf.getAwsSecretKey().get())
          )).withClientConfiguration(
              new ClientConfiguration()
                  .withMaxConnections(configuration.getMaxConcurrentWebhooks())
          )
          .withRegion(webhookConf.getAwsRegion().or("us-east-1"))
          .build();
    } else {
      this.snsClient = AmazonSNSClientBuilder.defaultClient();
    }
    this.webhookManager = webhookManager;
    this.publishSemaphore = AsyncSemaphore.newBuilder(configuration::getMaxConcurrentWebhooks, executorServiceFactory.get("webhook-publish-semaphore", 1)).build();
    this.publishExecutor = managedCachedThreadPoolFactory.get("webhook-publish");
    this.typeToArn = new ConcurrentHashMap<>();
  }

  public void requestHistoryEvent(SingularityRequestHistory requestUpdate) {
    publish(WebhookType.REQUEST, requestUpdate)
        .exceptionally((t) -> {
          LOG.warn("Could not publish event, will retry ({})", t.getMessage());
          try {
            webhookManager.saveRequestUpdateForRetry(requestUpdate);
          } catch (Throwable t2) {
            LOG.error("Could not save update to zk for retry, dropping", t2);
          }
          return null;
        });
  }

  public void taskHistoryUpdateEvent(SingularityTaskHistoryUpdate taskUpdate) {
    publishSemaphore.call(() ->
        CompletableFuture.runAsync(() -> {
          try {
            Optional<SingularityTask> task = taskHistoryHelper.getTask(taskUpdate.getTaskId());
            if (task.isPresent()) {
              SingularityTaskWebhook taskWebhook = new SingularityTaskWebhook(task.get(), taskUpdate);
              PublishRequest publishRequest = new PublishRequest(
                  getOrCreateSnsTopic(WebhookType.TASK),
                  objectMapper.writeValueAsString(taskWebhook)
              );
              PublishResult result = snsClient.publish(publishRequest);
              LOG.trace("Sent update {} with messageId {}", taskWebhook, result.getMessageId());
            }
          } catch (IOException ioe) {
            throw new RuntimeException(ioe);
          }
        }, publishExecutor)
    ).exceptionally((t) -> {
      LOG.warn("Could not publish event, will retry ({})", t.getMessage());
      try {
        webhookManager.saveTaskUpdateForRetry(taskUpdate);
      } catch (Throwable t2) {
        LOG.error("Could not save update to zk for retry, dropping", t2);
      }
      return null;
    });
  }

  public void deployHistoryEvent(SingularityDeployUpdate deployUpdate) {
    publish(WebhookType.DEPLOY, deployUpdate)
        .exceptionally((t) -> {
          LOG.warn("Could not publish event to sns, will retry later ({})", t.getMessage());
          try {
            webhookManager.saveDeployUpdateForRetry(deployUpdate);
          } catch (Throwable t2) {
            LOG.error("Could not save update to zk for retry, dropping", t2);
          }
          return null;
        });
  }


  private String getOrCreateSnsTopic(WebhookType type) {
    return typeToArn.computeIfAbsent(type, (t) -> {
      String topic = webhookConf.getSnsTopics().get(type);
      try {
        LOG.info("Attempting to create sns topic {}", topic);
        CreateTopicRequest createTopicRequest = new CreateTopicRequest(topic);
        CreateTopicResult createTopicResult = snsClient.createTopic(createTopicRequest);
        return createTopicResult.getTopicArn();
      } catch (Throwable th) {
        LOG.error("Could not create sns topic {}", topic, th);
        throw th;
      }
    });
  }

  <T> CompletableFuture<Void> publish(WebhookType type, T content) {
    try {
      return publishSemaphore.call(() ->
          CompletableFuture.runAsync(() -> {
            try {
              PublishRequest publishRequest = new PublishRequest(
                  getOrCreateSnsTopic(type),
                  objectMapper.writeValueAsString(content)
              );
              PublishResult result = snsClient.publish(publishRequest);
              LOG.trace("Sent update {} with messageId {}", content, result.getMessageId());
            } catch (IOException ioe) {
              throw new RuntimeException(ioe);
            }
          }, publishExecutor));
    } catch (Throwable t) {
      CompletableFuture<Void> f = new CompletableFuture<>();
      f.completeExceptionally(t);
      return f;
    }
  }
}
