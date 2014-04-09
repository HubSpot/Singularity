package com.hubspot.singularity;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

public class SingularityTaskIdHistory implements Comparable<SingularityTaskIdHistory> {

  private final SingularityTaskId taskId;
  private final long updatedAt;
  private final Optional<ExtendedTaskState> lastTaskState;
  
  public static SingularityTaskIdHistory fromTaskIdAndUpdates(SingularityTaskId taskId, Collection<SingularityTaskHistoryUpdate> updates) {
    ExtendedTaskState lastTaskState = null;
    long updatedAt = taskId.getStartedAt();
    
    if (updates != null && !updates.isEmpty()) {
      SingularityTaskHistoryUpdate lastUpdate = Ordering.natural().max(updates);
      lastTaskState = lastUpdate.getTaskState();
      updatedAt = lastUpdate.getTimestamp();
    }
    
    return new SingularityTaskIdHistory(taskId, updatedAt, Optional.fromNullable(lastTaskState));
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
        .compare(updatedAt, o.getUpdatedAt())
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

  @Override
  public String toString() {
    return "SingularityTaskIdHistory [taskId=" + taskId + ", updatedAt=" + updatedAt + ", lastTaskState=" + lastTaskState + "]";
  }
  
}
