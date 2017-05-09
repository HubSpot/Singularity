package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityTaskCleanup {

  private final Optional<String> user;
  private final TaskCleanupType cleanupType;
  private final long timestamp;
  private final SingularityTaskId taskId;
  private final Optional<String> message;
  private final Optional<String> actionId;
  private final Optional<SingularityTaskShellCommandRequestId> runBeforeKillId;

  @JsonCreator
  public SingularityTaskCleanup(@JsonProperty("user") Optional<String> user, @JsonProperty("cleanupType") TaskCleanupType cleanupType, @JsonProperty("timestamp") long timestamp,
      @JsonProperty("taskId") SingularityTaskId taskId, @JsonProperty("message") Optional<String> message, @JsonProperty("actionId") Optional<String> actionId,
      @JsonProperty("runBeforeKillId") Optional<SingularityTaskShellCommandRequestId> runBeforeKillId) {
    this.user = user;
    this.cleanupType = cleanupType;
    this.timestamp = timestamp;
    this.taskId = taskId;
    this.message = message;
    this.actionId = actionId;
    this.runBeforeKillId = runBeforeKillId;
  }

  public Optional<String> getActionId() {
    return actionId;
  }

  public Optional<String> getMessage() {
    return message;
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

  public Optional<SingularityTaskShellCommandRequestId> getRunBeforeKillId() {
    return runBeforeKillId;
  }

  @Override
  public String toString() {
    return "SingularityTaskCleanup{" +
        "user=" + user +
        ", cleanupType=" + cleanupType +
        ", timestamp=" + timestamp +
        ", taskId=" + taskId +
        ", message=" + message +
        ", actionId=" + actionId +
        ", runBeforeKillId=" + runBeforeKillId +
        '}';
  }
}
