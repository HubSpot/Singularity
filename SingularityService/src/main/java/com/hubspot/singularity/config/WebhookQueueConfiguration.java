package com.hubspot.singularity.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.hubspot.singularity.WebhookType;
import com.hubspot.singularity.hooks.WebhookQueueType;
import java.util.Map;
import java.util.Optional;

public class WebhookQueueConfiguration {
  @JsonProperty
  private WebhookQueueType queueType = WebhookQueueType.ZOOKEEPER;

  @JsonProperty
  private Map<WebhookType, String> snsTopics = ImmutableMap.of(
    WebhookType.TASK,
    "singularity-task-updates",
    WebhookType.DEPLOY,
    "singularity-deploy-updates",
    WebhookType.REQUEST,
    "singularity-request-updates"
  );

  @JsonProperty
  private Optional<String> awsAccessKey = Optional.empty();

  @JsonProperty
  private Optional<String> awsSecretKey = Optional.empty();

  @JsonProperty
  private Optional<String> awsRegion = Optional.empty();

  private int snsRequestTimeout = 3000;

  private int snsSocketTimeout = 3000;

  private int snsConnectTimeout = 2000;

  private int snsTotalTimeout = 5000;

  // Protection for zookeeper so large list children calls will not take it down
  private int maxZkQueuedWebhooksPerParentNode = 3000;

  public WebhookQueueType getQueueType() {
    return queueType;
  }

  public void setQueueType(WebhookQueueType queueType) {
    this.queueType = queueType;
  }

  public Map<WebhookType, String> getSnsTopics() {
    return snsTopics;
  }

  public void setSnsTopics(Map<WebhookType, String> snsTopics) {
    this.snsTopics = snsTopics;
  }

  public Optional<String> getAwsAccessKey() {
    return awsAccessKey;
  }

  public void setAwsAccessKey(Optional<String> awsAccessKey) {
    this.awsAccessKey = awsAccessKey;
  }

  public Optional<String> getAwsSecretKey() {
    return awsSecretKey;
  }

  public void setAwsSecretKey(Optional<String> awsSecretKey) {
    this.awsSecretKey = awsSecretKey;
  }

  public Optional<String> getAwsRegion() {
    return awsRegion;
  }

  public void setAwsRegion(Optional<String> awsRegion) {
    this.awsRegion = awsRegion;
  }

  public int getSnsRequestTimeout() {
    return snsRequestTimeout;
  }

  public void setSnsRequestTimeout(int snsRequestTimeout) {
    this.snsRequestTimeout = snsRequestTimeout;
  }

  public int getSnsSocketTimeout() {
    return snsSocketTimeout;
  }

  public void setSnsSocketTimeout(int snsSocketTimeout) {
    this.snsSocketTimeout = snsSocketTimeout;
  }

  public int getSnsConnectTimeout() {
    return snsConnectTimeout;
  }

  public void setSnsConnectTimeout(int snsConnectTimeout) {
    this.snsConnectTimeout = snsConnectTimeout;
  }

  public int getSnsTotalTimeout() {
    return snsTotalTimeout;
  }

  public void setSnsTotalTimeout(int snsTotalTimeout) {
    this.snsTotalTimeout = snsTotalTimeout;
  }

  public int getMaxZkQueuedWebhooksPerParentNode() {
    return maxZkQueuedWebhooksPerParentNode;
  }

  public void setMaxZkQueuedWebhooksPerParentNode(int maxZkQueuedWebhooksPerParentNode) {
    this.maxZkQueuedWebhooksPerParentNode = maxZkQueuedWebhooksPerParentNode;
  }
}
