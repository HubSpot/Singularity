package com.hubspot.singularity;

import java.io.IOException;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SingularityRequestCleanup extends SingularityJsonObject {

  public enum RequestCleanupType {
    DELETING, PAUSING
  }

  private final Optional<String> user;
  private final RequestCleanupType cleanupType;
  private final Optional<Boolean> killTasks;
  private final long timestamp;
  private final String requestId;

  public static SingularityRequestCleanup fromBytes(byte[] bytes, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(bytes, SingularityRequestCleanup.class);
    } catch (IOException e) {
      throw new SingularityJsonException(e);
    }
  }

  @JsonCreator
  public SingularityRequestCleanup(@JsonProperty("user") Optional<String> user, @JsonProperty("cleanupType") RequestCleanupType cleanupType, @JsonProperty("timestamp") long timestamp,
      @JsonProperty("killTasks") Optional<Boolean> killTasks, @JsonProperty("requestId") String requestId) {
    this.user = user;
    this.cleanupType = cleanupType;
    this.timestamp = timestamp;
    this.requestId = requestId;
    this.killTasks = killTasks;
  }

  public Optional<Boolean> getKillTasks() {
    return killTasks;
  }

  public String getRequestId() {
    return requestId;
  }

  public Optional<String> getUser() {
    return user;
  }

  public RequestCleanupType getCleanupType() {
    return cleanupType;
  }

  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public String toString() {
    return "SingularityRequestCleanup [user=" + user + ", cleanupType=" + cleanupType + ", killTasks=" + killTasks + ", timestamp=" + timestamp + ", requestId=" + requestId + "]";
  }

}
