package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.mesos.JavaUtils;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representation of a destination for a specific type of webhook")
public class SingularityWebhook {

  private final String uri;
  private final WebhookType type;
  private final Optional<String> user;
  private final long timestamp;
  private final String id;

  @JsonCreator
  public SingularityWebhook(@JsonProperty("uri") String uri, @JsonProperty("timestamp") Optional<Long> timestamp, @JsonProperty("user") Optional<String> user, @JsonProperty("type") WebhookType type) {
    this.uri = uri;
    this.timestamp = timestamp.or(System.currentTimeMillis());
    this.user = user;
    this.id = type.name() + "-" + JavaUtils.urlEncode(uri);
    this.type = type;
  }

  @Schema(description = "Unique ID for webhook")
  public String getId() {
    return id;
  }

  @Schema(required = true, description = "URI to POST to")
  public String getUri() {
    return uri;
  }

  @Schema(description = "Webhook creation timestamp")
  public long getTimestamp() {
    return timestamp;
  }

  @Schema(description = "User that created webhook")
  public Optional<String> getUser() {
    return user;
  }

  @Schema(required = true, description = "Webhook type")
  public WebhookType getType() {
    return type;
  }

  @Override
  public String toString() {
    return "SingularityWebhook{" +
        "uri='" + uri + '\'' +
        ", type=" + type +
        ", user=" + user +
        ", timestamp=" + timestamp +
        ", id='" + id + '\'' +
        '}';
  }
}
