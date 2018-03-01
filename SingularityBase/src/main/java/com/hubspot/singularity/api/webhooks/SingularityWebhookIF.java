package com.hubspot.singularity.api.webhooks;

import java.util.Optional;

import org.immutables.value.Value.Immutable;

import com.hubspot.singularity.annotations.SingularityStyle;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema(description = "Representation of a destination for a specific type of webhook")
public interface SingularityWebhookIF {
  @Schema(description = "Unique ID for webhook")
  String getId();

  @Schema(required = true, description = "URI to POST to")
  String getUri();

  @Schema(description = "Webhook creation timestamp")
  long getTimestamp();

  @Schema(description = "User that created webhook")
  Optional<String> getUser();

  @Schema(required = true, description = "Webhook type")
  WebhookType getType();
}
