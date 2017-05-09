package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.mesos.JavaUtils;
import com.wordnik.swagger.annotations.ApiModelProperty;

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

  @ApiModelProperty(required=false, value="Unique ID for webhook.")
  public String getId() {
    return id;
  }

  @ApiModelProperty("URI to POST to.")
  public String getUri() {
    return uri;
  }

  @ApiModelProperty(required=false, value="")
  public long getTimestamp() {
    return timestamp;
  }

  @ApiModelProperty(required=false, value="User that created webhook.")
  public Optional<String> getUser() {
    return user;
  }

  @ApiModelProperty("Webhook type.")
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
