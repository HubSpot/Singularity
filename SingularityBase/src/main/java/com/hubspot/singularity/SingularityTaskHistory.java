package com.hubspot.singularity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityTaskHistory {

  private final List<SingularityTaskHistoryUpdate> taskUpdates;
  private final long timestamp;
  private final SingularityTask task;

  @JsonCreator
  public SingularityTaskHistory(@JsonProperty("taskUpdates") List<SingularityTaskHistoryUpdate> taskUpdates, @JsonProperty("timestamp") long timestamp, @JsonProperty("task") SingularityTask task) {
    this.taskUpdates = taskUpdates;
    this.timestamp = timestamp;
    this.task = task;
  }

  public List<SingularityTaskHistoryUpdate> getTaskUpdates() {
    return taskUpdates;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public SingularityTask getTask() {
    return task;
  }

  @Override
  public String toString() {
    return "SingularityTaskHistory [taskUpdates=" + taskUpdates + ", timestamp=" + timestamp + ", task=" + task + "]";
  }

}
