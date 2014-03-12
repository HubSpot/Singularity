package com.hubspot.singularity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityTaskHistory extends SingularityJsonObject {

  private final List<SingularityTaskHistoryUpdate> taskUpdates;
  private final SingularityTaskState taskState;
  private final SingularityTask task;

  @JsonCreator
  public SingularityTaskHistory(@JsonProperty("taskUpdates") List<SingularityTaskHistoryUpdate> taskUpdates, @JsonProperty("taskState") SingularityTaskState taskState, @JsonProperty("task") SingularityTask task) {
    this.taskUpdates = taskUpdates;
    this.taskState = taskState;
    this.task = task;
  }

  public List<SingularityTaskHistoryUpdate> getTaskUpdates() {
    return taskUpdates;
  }

  public SingularityTaskState getTaskState() {
    return taskState;
  }
  
  public SingularityTask getTask() {
    return task;
  }

  @Override
  public String toString() {
    return "SingularityTaskHistory [taskUpdates=" + taskUpdates + ", taskState=" + taskState + ", task=" + task + "]";
  }
  
}
