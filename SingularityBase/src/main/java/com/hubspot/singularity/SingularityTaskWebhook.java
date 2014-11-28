package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityTaskWebhook {

  private final SingularityTask task;
  private final SingularityTaskHistoryUpdate taskUpdate;

  @JsonCreator
  public SingularityTaskWebhook(@JsonProperty("task") SingularityTask task, @JsonProperty("taskUpdate") SingularityTaskHistoryUpdate taskUpdate) {
    this.task = task;
    this.taskUpdate = taskUpdate;
  }

  public SingularityTask getTask() {
    return task;
  }

  public SingularityTaskHistoryUpdate getTaskUpdate() {
    return taskUpdate;
  }

  @Override
  public String toString() {
    return "SingularityTaskWebhook [task=" + task + ", taskUpdate=" + taskUpdate + "]";
  }

}
