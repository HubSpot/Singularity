package com.hubspot.singularity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityTaskHistory {

  private final List<SingularityTaskHistoryUpdate> taskUpdates;
  private final long timestamp;
  private final Optional<String> directory;
  private final SingularityTask task;

  @JsonCreator
  public SingularityTaskHistory(@JsonProperty("taskUpdates") List<SingularityTaskHistoryUpdate> taskUpdates, @JsonProperty("timestamp") long timestamp, @JsonProperty("task") SingularityTask task, @JsonProperty("directory") Optional<String> directory) {
    this.taskUpdates = taskUpdates;
    this.timestamp = timestamp;
    this.task = task;
    this.directory = directory;
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
  
  public Optional<String> getDirectory() {
    return directory;
  }

  @Override
  public String toString() {
    return "SingularityTaskHistory [taskUpdates=" + taskUpdates + ", timestamp=" + timestamp + ", directory=" + directory + ", task=" + task + "]";
  }

}
