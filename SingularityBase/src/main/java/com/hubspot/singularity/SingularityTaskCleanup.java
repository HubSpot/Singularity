package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

public class SingularityTaskCleanup extends SingularityJsonObject {

  public enum TaskCleanupType {
    USER_REQUESTED, DECOMISSIONING, SCALING_DOWN, BOUNCING, DEPLOY_FAILED, NEW_DEPLOY_SUCCEEDED
  }
  
  private final Optional<String> user;
  private final TaskCleanupType cleanupType;
  private final long timestamp;
  private final SingularityTaskId taskId;
  
  public static SingularityTaskCleanup fromBytes(byte[] bytes, ObjectMapper objectMapper) throws Exception {
    return objectMapper.readValue(bytes, SingularityTaskCleanup.class);
  }
  
  @JsonCreator
  public SingularityTaskCleanup(@JsonProperty("user") Optional<String> user, @JsonProperty("cleanupType") String cleanupType, @JsonProperty("timestamp") long timestamp, @JsonProperty("taskId") SingularityTaskId taskId) {
    this(user, TaskCleanupType.valueOf(cleanupType), timestamp, taskId);
  }
   
  public SingularityTaskCleanup(Optional<String> user, TaskCleanupType cleanupType, long timestamp, SingularityTaskId taskId) {
    this.user = user;
    this.cleanupType = cleanupType;
    this.timestamp = timestamp;
    this.taskId = taskId;
  }

  public Optional<String> getUser() {
    return user;
  }

  @JsonIgnore
  public TaskCleanupType getCleanupTypeEnum() {
    return cleanupType;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public SingularityTaskId getTaskId() {
    return taskId;
  }
  
  public String getCleanupType() {
    return cleanupType.name();
  }
  
  @Override
  public String toString() {
    return "SingularityTaskCleanup [user=" + user + ", cleanupType=" + cleanupType + ", timestamp=" + timestamp + ", taskId=" + taskId + "]";
  }
  
}
