package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityTaskCleanup {

  public enum TaskCleanupType {
    USER_REQUESTED(true, true), DECOMISSIONING(false, false), SCALING_DOWN(true, false), BOUNCING(false, false), DEPLOY_FAILED(true, true), NEW_DEPLOY_SUCCEEDED(true, false), DEPLOY_CANCELED(true, true), UNHEALTHY_NEW_TASK(true, true), OVERDUE_NEW_TASK(true, true);

    private final boolean killLongRunningTaskInstantly;
    private final boolean killNonLongRunningTaskInstantly;

    private TaskCleanupType(boolean killLongRunningTaskInstantly, boolean killNonLongRunningTaskInstantly) {
      this.killLongRunningTaskInstantly = killLongRunningTaskInstantly;
      this.killNonLongRunningTaskInstantly = killNonLongRunningTaskInstantly;
    }

    public boolean shouldKillTaskInstantly(SingularityRequest request) {
      if (request.isLongRunning()) {
        return killLongRunningTaskInstantly;
      } else {
        return killNonLongRunningTaskInstantly;
      }
    }
  }

  private final Optional<String> user;
  private final TaskCleanupType cleanupType;
  private final long timestamp;
  private final SingularityTaskId taskId;

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
