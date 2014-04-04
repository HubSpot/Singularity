package com.hubspot.singularity;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

public class SingularityRequestCleanup extends SingularityJsonObject {

  public enum RequestCleanupType {
    DELETING, PAUSING
  }
  
  private final Optional<String> user;
  private final RequestCleanupType cleanupType;
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
  public SingularityRequestCleanup(@JsonProperty("user") Optional<String> user, @JsonProperty("cleanupType") RequestCleanupType cleanupType, @JsonProperty("timestamp") long timestamp, @JsonProperty("requestId") String requestId) {
    this.user = user;
    this.cleanupType = cleanupType;
    this.timestamp = timestamp;
    this.requestId = requestId;
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
    return "SingularityRequestCleanup [user=" + user + ", cleanupType=" + cleanupType + ", timestamp=" + timestamp + ", requestId=" + requestId + "]";
  }
  
}
