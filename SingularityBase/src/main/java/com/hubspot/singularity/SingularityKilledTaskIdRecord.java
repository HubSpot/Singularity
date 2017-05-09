package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

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

  public SingularityTaskId getTaskId() {
    return taskId;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public Optional<RequestCleanupType> getRequestCleanupType() {
    return requestCleanupType;
  }

  public Optional<TaskCleanupType> getTaskCleanupType() {
    return taskCleanupType;
  }

  public long getOriginalTimestamp() {
    return originalTimestamp;
  }

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
