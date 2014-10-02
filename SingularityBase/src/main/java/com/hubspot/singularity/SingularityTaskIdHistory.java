package com.hubspot.singularity;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Iterables;

public class SingularityTaskIdHistory implements Comparable<SingularityTaskIdHistory> {

  private final SingularityTaskId taskId;
  private final long updatedAt;
  private final Optional<ExtendedTaskState> lastTaskState;

  public static SingularityTaskIdHistory fromTaskIdAndUpdates(SingularityTaskId taskId, List<SingularityTaskHistoryUpdate> updates) {
    ExtendedTaskState lastTaskState = null;
    long updatedAt = taskId.getStartedAt();

    if (updates != null && !updates.isEmpty()) {
      SingularityTaskHistoryUpdate lastUpdate = Iterables.getLast(updates);
      lastTaskState = lastUpdate.getTaskState();
      updatedAt = lastUpdate.getTimestamp();
    }

    return new SingularityTaskIdHistory(taskId, updatedAt, Optional.ofNullable(lastTaskState));
  }

  @JsonCreator
  public SingularityTaskIdHistory(@JsonProperty("taskId") SingularityTaskId taskId, @JsonProperty("updatedAt") long updatedAt, @JsonProperty("lastStatus") Optional<ExtendedTaskState> lastTaskState) {
    this.taskId = taskId;
    this.updatedAt = updatedAt;
    this.lastTaskState = lastTaskState;
  }

  @Override
  public int compareTo(SingularityTaskIdHistory o) {
    return ComparisonChain.start()
        .compare(o.getUpdatedAt(), updatedAt)
        .compare(taskId.getId(), o.getTaskId().getId())
        .result();
  }

  @Override
  public int hashCode() {
      return Objects.hashCode(taskId, updatedAt, lastTaskState);
  }

  @Override
  public boolean equals(Object other) {
      if (other == this) {
          return true;
      }
      if (other == null || other.getClass() != this.getClass()) {
          return false;
      }

      SingularityTaskIdHistory that = (SingularityTaskIdHistory) other;
      return Objects.equal(this.taskId , that.taskId)
              && Objects.equal(this.updatedAt , that.updatedAt)
              && Objects.equal(this.lastTaskState , that.lastTaskState);
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

  @Override
  public String toString() {
    return "SingularityTaskIdHistory [taskId=" + taskId + ", updatedAt=" + updatedAt + ", lastTaskState=" + lastTaskState + "]";
  }

}
