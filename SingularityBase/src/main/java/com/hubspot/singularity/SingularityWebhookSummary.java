package com.hubspot.singularity;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityWebhookSummary {
  private final SingularityWebhook webhook;
  private final int queueSize;

  @JsonCreator
  public SingularityWebhookSummary(@JsonProperty("webhook") SingularityWebhook webhook, @JsonProperty("queueSize") int queueSize) {
    this.webhook = webhook;
    this.queueSize = queueSize;
  }

  public SingularityWebhook getWebhook() {
    return webhook;
  }

  public int getQueueSize() {
    return queueSize;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityWebhookSummary that = (SingularityWebhookSummary) o;
    return queueSize == that.queueSize &&
        Objects.equals(webhook, that.webhook);
  }

  @Override
  public int hashCode() {
    return Objects.hash(webhook, queueSize);
  }

  @Override
  public String toString() {
    return "SingularityWebhookSummary{" +
        "webhook=" + webhook +
        ", queueSize=" + queueSize +
        '}';
  }
}
