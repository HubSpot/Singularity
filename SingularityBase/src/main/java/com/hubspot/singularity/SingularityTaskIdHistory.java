package com.hubspot.singularity;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.ComparisonChain;

public class SingularityTaskIdHistory implements Comparable<SingularityTaskIdHistory> {

  private final SingularityTaskId taskId;
  private final long updatedAt;
  private final Optional<ExtendedTaskState> lastTaskState;
  private final Optional<String> runId;

  public static SingularityTaskIdHistory fromTaskIdAndTaskAndUpdates(SingularityTaskId taskId, SingularityTask task, List<SingularityTaskHistoryUpdate> updates) {
    ExtendedTaskState lastTaskState = null;
    long updatedAt = taskId.getStartedAt();

    if (updates != null && !updates.isEmpty()) {
      SingularityTaskHistoryUpdate lastUpdate = Collections.max(updates);
      lastTaskState = lastUpdate.getTaskState();
      updatedAt = lastUpdate.getTimestamp();
    }

    return new SingularityTaskIdHistory(taskId, updatedAt, Optional.fromNullable(lastTaskState), task.getTaskRequest().getPendingTask().getRunId());
  }

  @JsonCreator
  public SingularityTaskIdHistory(@JsonProperty("taskId") SingularityTaskId taskId, @JsonProperty("updatedAt") long updatedAt,
      @JsonProperty("lastStatus") Optional<ExtendedTaskState> lastTaskState, @JsonProperty("runId") Optional<String> runId) {
    this.taskId = taskId;
    this.updatedAt = updatedAt;
    this.lastTaskState = lastTaskState;
    this.runId = runId;
  }

  @Override
  public int compareTo(SingularityTaskIdHistory o) {
    return ComparisonChain.start()
      .compare(o.getUpdatedAt(), updatedAt)
      .compare(taskId.getId(), o.getTaskId().getId())
      .result();
  }

  public SingularityTaskId getTaskId() {
    return taskId;
  }

  public Optional<ExtendedTaskState> getLastTaskState() {
    return lastTaskState;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public Optional<String> getRunId() {
    return runId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityTaskIdHistory that = (SingularityTaskIdHistory) o;
    return updatedAt == that.updatedAt &&
        Objects.equals(taskId, that.taskId) &&
        Objects.equals(lastTaskState, that.lastTaskState) &&
        Objects.equals(runId, that.runId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(taskId, updatedAt, lastTaskState, runId);
  }

  @Override
  public String toString() {
    return "SingularityTaskIdHistory{" +
        "taskId=" + taskId +
        ", updatedAt=" + updatedAt +
        ", lastTaskState=" + lastTaskState +
        ", runId=" + runId +
        '}';
  }
}
