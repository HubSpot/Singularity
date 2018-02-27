package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Describes a task that has been sent a kill signal")
public class SingularityKilledTaskIdRecord {

  private final SingularityTaskId taskId;
  private final long originalTimestamp;
  private final long timestamp;
  private final Optional<RequestCleanupType> requestCleanupType;
  private final Optional<TaskCleanupType> taskCleanupType;
  private final int retries;

  @JsonCreator
  public SingularityKilledTaskIdRecord(@JsonProperty("taskId") SingularityTaskId taskId, @JsonProperty("timestamp") long timestamp, @JsonProperty("originalTimestamp") long originalTimestamp,
      @JsonProperty("requestCleanupType") Optional<RequestCleanupType> requestCleanupType, @JsonProperty("taskCleanupType") Optional<TaskCleanupType> taskCleanupType,
      @JsonProperty("retries") int retries) {
    this.taskId = taskId;
    this.timestamp = timestamp;
    this.requestCleanupType = requestCleanupType;
    this.taskCleanupType = taskCleanupType;
    this.retries = retries;
    this.originalTimestamp = originalTimestamp;
  }

  @Schema(description = "The unique id of the task")
  public SingularityTaskId getTaskId() {
    return taskId;
  }

  @Schema(description = "the time at which the signal was triggered")
  public long getTimestamp() {
    return timestamp;
  }

  @Schema(description = "An optional enum cleanup type associated with this task kill", nullable = true)
  public Optional<RequestCleanupType> getRequestCleanupType() {
    return requestCleanupType;
  }

  @Schema(description = "An optional enum cleanup type associated with this task kill", nullable = true)
  public Optional<TaskCleanupType> getTaskCleanupType() {
    return taskCleanupType;
  }

  @Schema(description = "The original time when the task kill was triggered (in case multiple kills have been issued)")
  public long getOriginalTimestamp() {
    return originalTimestamp;
  }

  @Schema(description = "The number of attempts to kill this task so far")
  public int getRetries() {
    return retries;
  }

  @Override
  public String toString() {
    return "SingularityKilledTaskIdRecord{" +
        "taskId=" + taskId +
        ", originalTimestamp=" + originalTimestamp +
        ", timestamp=" + timestamp +
        ", requestCleanupType=" + requestCleanupType +
        ", taskCleanupType=" + taskCleanupType +
        ", retries=" + retries +
        '}';
  }
}
