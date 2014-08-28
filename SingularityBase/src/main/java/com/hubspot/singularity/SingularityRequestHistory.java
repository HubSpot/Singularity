package com.hubspot.singularity;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ComparisonChain;

public class SingularityRequestHistory extends SingularityJsonObject implements Comparable<SingularityRequestHistory> {

  private final long createdAt;
  private final Optional<String> user;
  private final RequestHistoryType eventType;
  private final SingularityRequest request;

  public enum RequestHistoryType {
    CREATED, UPDATED, DELETED, PAUSED, UNPAUSED, ENTERED_COOLDOWN, EXITED_COOLDOWN, FINISHED;
  }

  public static SingularityRequestHistory fromBytes(byte[] bytes, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(bytes, SingularityRequestHistory.class);
    } catch (IOException e) {
      throw new SingularityJsonException(e);
    }
  }

  @JsonCreator
  public SingularityRequestHistory(@JsonProperty("createdAt") long createdAt, @JsonProperty("user") Optional<String> user, @JsonProperty("eventType") RequestHistoryType eventType, @JsonProperty("request") SingularityRequest request) {
    this.createdAt = createdAt;
    this.user = user;
    this.eventType = eventType;
    this.request = request;
  }

  @Override
  public int compareTo(SingularityRequestHistory o) {
    return ComparisonChain
        .start()
        .compare(createdAt, o.getCreatedAt())
        .compare(request.getId(), o.getRequest().getId())
        .result();
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public Optional<String> getUser() {
    return user;
  }

  @Deprecated
  public RequestHistoryType getState() {
    return eventType;
  }

  public RequestHistoryType getEventType() {
    return eventType;
  }

  public SingularityRequest getRequest() {
    return request;
  }

  @Override
  public String toString() {
    return "SingularityRequestHistory [createdAt=" + createdAt + ", user=" + user + ", eventType=" + eventType + ", request=" + request + "]";
  }

}
