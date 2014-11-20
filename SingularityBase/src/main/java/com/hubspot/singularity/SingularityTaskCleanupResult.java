package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityTaskCleanupResult {

  private final SingularityCreateResult result;
  private final SingularityTask task;

  @JsonCreator
  public SingularityTaskCleanupResult(@JsonProperty("result") SingularityCreateResult result, @JsonProperty("task") SingularityTask task) {
    this.result = result;
    this.task = task;
  }

  public SingularityCreateResult getResult() {
    return result;
  }

  public SingularityTask getTask() {
    return task;
  }

  @Override
  public String toString() {
    return "SingularityTaskCleanupResult [result=" + result + ", taskId=" + task.getTaskId() + "]";
  }
}
