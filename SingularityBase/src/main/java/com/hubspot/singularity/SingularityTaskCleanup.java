package com.hubspot.singularity;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

public class SingularityTaskCleanup extends SingularityJsonObject {

  public enum TaskCleanupType {
    USER_REQUESTED(true), DECOMISSIONING(false), SCALING_DOWN(true), BOUNCING(false), DEPLOY_FAILED(true), NEW_DEPLOY_SUCCEEDED(true), DEPLOY_CANCELED(true), UNHEALTHY_NEW_TASK(true), OVERDUE_NEW_TASK(true);
  
    private final boolean killInstantly;
    
    private TaskCleanupType(boolean killInstantly) {
      this.killInstantly = killInstantly;
    }
    
    public boolean shouldKillInstantly() {
      return killInstantly;
    }
  }
  
  private final Optional<String> user;
  private final TaskCleanupType cleanupType;
  private final long timestamp;
  private final SingularityTaskId taskId;
  
  public static SingularityTaskCleanup fromBytes(byte[] bytes, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(bytes, SingularityTaskCleanup.class);
    } catch (IOException e) {
      throw new SingularityJsonException(e);
    }
  }
  
  @JsonCreator
  public SingularityTaskCleanup(@JsonProperty("user") Optional<String> user, @JsonProperty("cleanupType") TaskCleanupType cleanupType, @JsonProperty("timestamp") long timestamp, @JsonProperty("taskId") SingularityTaskId taskId) {
    this.user = user;
    this.cleanupType = cleanupType;
    this.timestamp = timestamp;
    this.taskId = taskId;
  }
  
  public Optional<String> getUser() {
    return user;
  }

  public TaskCleanupType getCleanupType() {
    return cleanupType;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public SingularityTaskId getTaskId() {
    return taskId;
  }
    
  @Override
  public String toString() {
    return "SingularityTaskCleanup [user=" + user + ", cleanupType=" + cleanupType + ", timestamp=" + timestamp + ", taskId=" + taskId + "]";
  }
  
}
