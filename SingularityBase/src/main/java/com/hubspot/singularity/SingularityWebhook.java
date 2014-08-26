package com.hubspot.singularity;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.hubspot.mesos.JavaUtils;

public class SingularityWebhook extends SingularityJsonObject {

  private final String uri;
  private final long timestamp;
  private final String id;
  private final Optional<String> user;
  private final WebhookType type;
  
  public static SingularityWebhook fromBytes(byte[] bytes, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(bytes, SingularityWebhook.class);
    } catch (IOException e) {
      throw new SingularityJsonException(e);
    }
  }

  @JsonCreator
  public SingularityWebhook(@JsonProperty("uri") String uri, @JsonProperty("timestamp") long timestamp, @JsonProperty("user") Optional<String> user, @JsonProperty("type") WebhookType type) {
    this.uri = uri;
    this.timestamp = timestamp;
    this.user = user;
    this.id = JavaUtils.urlEncode(uri);
    this.type = type;
  }

  public String getId() {
    return id;
  }

  public String getUri() {
    return uri;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public Optional<String> getUser() {
    return user;
  }

  public WebhookType getType() {
    return type;
  }

  @Override
  public String toString() {
    return "SingularityWebhook [uri=" + uri + ", timestamp=" + timestamp + ", id=" + id + ", user=" + user + ", type=" + type + "]";
  }

}