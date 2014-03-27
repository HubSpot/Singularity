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
  private final Optional<String> lastStatus;
  
  public static SingularityTaskIdHistory fromTaskIdAndUpdates(SingularityTaskId taskId, Collection<SingularityTaskHistoryUpdate> updates) {
    String lastTaskStatus = null;
    long updatedAt = taskId.getStartedAt();
    
    if (updates != null && !updates.isEmpty()) {
      SingularityTaskHistoryUpdate lastUpdate = Ordering.natural().max(updates);
      lastTaskStatus = lastUpdate.getStatusUpdate();
      updatedAt = lastUpdate.getTimestamp();
    }
    
    return new SingularityTaskIdHistory(taskId, updatedAt, Optional.fromNullable(lastTaskStatus));
  }
  
  @JsonCreator
  public SingularityTaskIdHistory(@JsonProperty("taskId") SingularityTaskId taskId, @JsonProperty("updatedAt") long updatedAt, @JsonProperty("lastStatus") Optional<String> lastStatus) {
    this.taskId = taskId;
    this.updatedAt = updatedAt;
    this.lastStatus = lastStatus;
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

  public Optional<String> getLastStatus() {
    return lastStatus;
  }
  
  public long getUpdatedAt() {
    return updatedAt;
  }

  @Override
  public String toString() {
    return "SingularityTaskIdHistory [taskId=" + taskId + ", updatedAt=" + updatedAt + ", lastStatus=" + lastStatus + "]";
  }
  
}
