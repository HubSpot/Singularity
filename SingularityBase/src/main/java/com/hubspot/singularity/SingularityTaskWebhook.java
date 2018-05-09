package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A webhook sent for a task status update")
public class SingularityTaskWebhook {

  private final SingularityTask task;
  private final SingularityTaskHistoryUpdate taskUpdate;

  @JsonCreator
  public SingularityTaskWebhook(@JsonProperty("task") SingularityTask task, @JsonProperty("taskUpdate") SingularityTaskHistoryUpdate taskUpdate) {
    this.task = task;
    this.taskUpdate = taskUpdate;
  }

  @Schema(description = "The task this webhook refers to.")
  public SingularityTask getTask() {
    return task;
  }

  @Schema(description = "The task history update this webhook refers to.")
  public SingularityTaskHistoryUpdate getTaskUpdate() {
    return taskUpdate;
  }

  @Override
  public String toString() {
    return "SingularityTaskWebhook{" +
        "task=" + task +
        ", taskUpdate=" + taskUpdate +
        '}';
  }
}
