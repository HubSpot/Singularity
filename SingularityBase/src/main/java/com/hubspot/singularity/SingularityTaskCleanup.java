package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Describes the cleanup or shutdown of a singularity task")
public class SingularityTaskCleanup {

  private final Optional<String> user;
  private final TaskCleanupType cleanupType;
  private final long timestamp;
  private final SingularityTaskId taskId;
  private final Optional<String> message;
  private final Optional<String> actionId;
  private final Optional<SingularityTaskShellCommandRequestId> runBeforeKillId;
  private final Optional<Boolean> removeFromLoadBalancer;

  @JsonCreator
  public SingularityTaskCleanup(@JsonProperty("user") Optional<String> user, @JsonProperty("cleanupType") TaskCleanupType cleanupType, @JsonProperty("timestamp") long timestamp,
                                @JsonProperty("taskId") SingularityTaskId taskId, @JsonProperty("message") Optional<String> message, @JsonProperty("actionId") Optional<String> actionId,
                                @JsonProperty("runBeforeKillId") Optional<SingularityTaskShellCommandRequestId> runBeforeKillId,
                                @JsonProperty("removeFromLoadBalancer") Optional<Boolean> removeFromLoadBalancer
  ) {
    this.user = user;
    this.cleanupType = cleanupType;
    this.timestamp = timestamp;
    this.taskId = taskId;
    this.message = message;
    this.actionId = actionId;
    this.runBeforeKillId = runBeforeKillId;
    this.removeFromLoadBalancer = removeFromLoadBalancer;
  }

  public SingularityTaskCleanup(Optional<String> user,
                                TaskCleanupType cleanupType,
                                long timestamp,
                                SingularityTaskId taskId,
                                Optional<String> message,
                                Optional<String> actionId,
                                Optional<SingularityTaskShellCommandRequestId> runBeforeKillId) {
    this(user, cleanupType, timestamp, taskId, message, actionId, runBeforeKillId, Optional.absent());
  }

  @Schema(description = "An optional unique id associted with the cleanup of this task", nullable = true)
  public Optional<String> getActionId() {
    return actionId;
  }

  @Schema(description = "An optional message describing the reason this task was cleaned", nullable = true)
  public Optional<String> getMessage() {
    return message;
  }

  @Schema(description = "The user who triggered this cleanup", nullable = true)
  public Optional<String> getUser() {
    return user;
  }

  @Schema(description = "The enum reason for this cleanup")
  public TaskCleanupType getCleanupType() {
    return cleanupType;
  }

  @Schema(description = "The time this cleanup was created")
  public long getTimestamp() {
    return timestamp;
  }

  @Schema(description = "The unique id of the task being cleaned up")
  public SingularityTaskId getTaskId() {
    return taskId;
  }

  @Schema(description = "An optional command to run before shutting down the task")
  public Optional<SingularityTaskShellCommandRequestId> getRunBeforeKillId() {
    return runBeforeKillId;
  }

  @Schema(
      title = "If a request is being deleted and this is the final task, trigger deletion of the request from the load balancer",
      defaultValue = "true"
  )
  public Optional<Boolean> getRemoveFromLoadBalancer() {
    return removeFromLoadBalancer;
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
