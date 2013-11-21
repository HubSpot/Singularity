package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

public class SingularityTaskCleanup extends SingularityJsonObject {

  public enum CleanupType {
    USER_REQUESTED, DECOMISSIONING
  }
  
  private final Optional<String> user;
  private final CleanupType cleanupType;
  private final long timestamp;
  private final String taskId;
  private final String requestId;
  
  @JsonCreator
  public SingularityTaskCleanup(@JsonProperty("user") Optional<String> user, @JsonProperty("cleanupType") String cleanupType, @JsonProperty("timestamp") long timestamp, @JsonProperty("taskId") String taskId, @JsonProperty("requestId") String requestId) {
    this(user, CleanupType.valueOf(cleanupType), timestamp, taskId, requestId);
  }
   
  public SingularityTaskCleanup(Optional<String> user, CleanupType cleanupType, long timestamp, String taskId, String requestId) {
    this.user = user;
    this.cleanupType = cleanupType;
    this.timestamp = timestamp;
    this.taskId = taskId;
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

  public String getTaskId() {
    return taskId;
  }
  
  public String getCleanupType() {
    return cleanupType.name();
  }
  
  public static SingularityTaskCleanup fromBytes(byte[] bytes, ObjectMapper objectMapper) throws Exception {
    return objectMapper.readValue(bytes, SingularityTaskCleanup.class);
  }

  @Override
  public String toString() {
    return "SingularityTaskCleanup [user=" + user + ", cleanupType=" + cleanupType + ", timestamp=" + timestamp + ", taskId=" + taskId + ", requestId=" + requestId + "]";
  }  
  
}
