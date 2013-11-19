package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityTaskIdHistory {

  private final SingularityTaskId taskId;
  private final Optional<String> lastStatus;
  private final long createdAt;
  private final Optional<Long> updatedAt;

  @JsonCreator
  public SingularityTaskIdHistory(@JsonProperty("taskId") SingularityTaskId taskId, @JsonProperty("lastStatus") Optional<String> lastStatus, @JsonProperty("createdAt") long createdAt, @JsonProperty("updatedAt") Optional<Long> updatedAt) {
    this.taskId = taskId;
    this.lastStatus = lastStatus;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public SingularityTaskId getTaskId() {
    return taskId;
  }

  public Optional<String> getLastStatus() {
    return lastStatus;
  }

  public Optional<Long> getUpdatedAt() {
    return updatedAt;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  @Override
  public String toString() {
    return "SingularityTaskIdHistory [taskId=" + taskId + ", lastStatus=" + lastStatus + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + "]";
  }

}
