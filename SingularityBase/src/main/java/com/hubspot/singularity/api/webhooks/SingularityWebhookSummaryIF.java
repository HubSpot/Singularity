package com.hubspot.singularity.api.webhooks;

import org.immutables.value.Value.Immutable;

import com.hubspot.singularity.annotations.SingularityStyle;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema(description = "Describes the settings and queue size for a webhook")
public interface SingularityWebhookSummaryIF {
  @Schema(description = "The description of the webhook")
  SingularityWebhook getWebhook();

  @Schema(description = "The number of pending webhook sends in the queue for this webhook destination")
  int getQueueSize();
}
