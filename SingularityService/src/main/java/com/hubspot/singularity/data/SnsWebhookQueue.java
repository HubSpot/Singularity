package com.hubspot.singularity.data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.WebhookType;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.config.WebhookQueueConfiguration;
import com.hubspot.singularity.event.SingularityEventListener;

@Singleton
public class SnsWebhookQueue implements SingularityEventListener {
  private static final Logger LOG = LoggerFactory.getLogger(SnsWebhookQueue.class);

  private final WebhookQueueConfiguration webhookConf;
  private final ObjectMapper objectMapper;
  private final AmazonSNS snsClient;

  private final Map<WebhookType, String> typeToArn;

  @Inject
  public SnsWebhookQueue(ObjectMapper objectMapper,
                         SingularityConfiguration configuration) {
    this.objectMapper = objectMapper;
    this.webhookConf = configuration.getWebhookQueueConfiguration();
    if (webhookConf.getAwsAccessKey().isPresent() && webhookConf.getAwsSecretKey().isPresent()) {
      this.snsClient = AmazonSNSClient.builder()
          .withCredentials(new AWSStaticCredentialsProvider(
              new BasicAWSCredentials(webhookConf.getAwsAccessKey().get(), webhookConf.getAwsSecretKey().get())
          ))
          .build();
    } else {
      this.snsClient = AmazonSNSClientBuilder.defaultClient();
    }
    this.typeToArn = new ConcurrentHashMap<>();
  }

  @Override
  public void requestHistoryEvent(SingularityRequestHistory requestUpdate) {
    try {
      PublishRequest publishRequest = new PublishRequest(
          getOrCreateSnsTopic(WebhookType.REQUEST),
          objectMapper.writeValueAsString(requestUpdate)
      );
      PublishResult publishResult = snsClient.publish(publishRequest);
      LOG.trace("Published update {} with messageId {}", requestUpdate, publishResult.getMessageId());
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  @Override
  public void taskHistoryUpdateEvent(SingularityTaskHistoryUpdate taskUpdate) {
    try {
      PublishRequest publishRequest = new PublishRequest(
          getOrCreateSnsTopic(WebhookType.TASK),
          objectMapper.writeValueAsString(taskUpdate)
      );
      PublishResult publishResult = snsClient.publish(publishRequest);
      LOG.trace("Published update {} with messageId {}", taskUpdate, publishResult.getMessageId());
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  @Override
  public void deployHistoryEvent(SingularityDeployUpdate deployUpdate) {
    try {
      PublishRequest publishRequest = new PublishRequest(
          getOrCreateSnsTopic(WebhookType.DEPLOY),
          objectMapper.writeValueAsString(deployUpdate)
      );
      PublishResult publishResult = snsClient.publish(publishRequest);
      LOG.trace("Published update {} with messageId {}", deployUpdate, publishResult.getMessageId());
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  private String getOrCreateSnsTopic(WebhookType type) {
    return typeToArn.computeIfAbsent(type, (t) -> {
      CreateTopicRequest createTopicRequest = new CreateTopicRequest(webhookConf.getSnsTopics().get(type));
      CreateTopicResult createTopicResult = snsClient.createTopic(createTopicRequest);
      return createTopicResult.getTopicArn();
    });
  }
}
