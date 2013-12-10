package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

public class SingularityRequestCleanup extends SingularityJsonObject {

  public enum CleanupType {
    DELETING, PAUSING
  }
  
  private final Optional<String> user;
  private final CleanupType cleanupType;
  private final long timestamp;
  private final String requestId;
  
  @JsonCreator
  public SingularityRequestCleanup(@JsonProperty("user") Optional<String> user, @JsonProperty("cleanupType") String cleanupType, @JsonProperty("timestamp") long timestamp, @JsonProperty("requestId") String requestId) {
    this(user, CleanupType.valueOf(cleanupType), timestamp, requestId);
  }
   
  public SingularityRequestCleanup(Optional<String> user, CleanupType cleanupType, long timestamp, String requestId) {
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

  @JsonIgnore
  public CleanupType getCleanupTypeEnum() {
    return cleanupType;
  }

  public long getTimestamp() {
    return timestamp;
  }
  
  public String getCleanupType() {
    return cleanupType.name();
  }
  
  public static SingularityRequestCleanup fromBytes(byte[] bytes, ObjectMapper objectMapper) throws Exception {
    return objectMapper.readValue(bytes, SingularityRequestCleanup.class);
  }

  @Override
  public String toString() {
    return "SingularityRequestCleanup [user=" + user + ", cleanupType=" + cleanupType + ", timestamp=" + timestamp + ", requestId=" + requestId + "]";
  }
  
}
