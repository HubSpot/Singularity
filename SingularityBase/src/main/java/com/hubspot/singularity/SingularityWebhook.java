package com.hubspot.singularity;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.hubspot.mesos.JavaUtils;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class SingularityWebhook extends SingularityJsonObject {

  private final String uri;
  private final WebhookType type;

  private final Optional<String> user;
  private final long timestamp;

  private final String id;

  public static SingularityWebhook fromBytes(byte[] bytes, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(bytes, SingularityWebhook.class);
    } catch (IOException e) {
      throw new SingularityJsonException(e);
    }
  }

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

  @ApiModelProperty("Webhook type (TASK, REQUEST, DEPLOY).")
  public WebhookType getType() {
    return type;
  }

  @Override
  public String toString() {
    return "SingularityWebhook [uri=" + uri + ", timestamp=" + timestamp + ", id=" + id + ", user=" + user + ", type=" + type + "]";
  }
}
