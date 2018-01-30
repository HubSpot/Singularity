package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class SingularityTaskWebhook {

  private final SingularityTask task;
  private final SingularityTaskHistoryUpdate taskUpdate;

  @JsonCreator
  public SingularityTaskWebhook(@JsonProperty("task") SingularityTask task, @JsonProperty("taskUpdate") SingularityTaskHistoryUpdate taskUpdate) {
    this.task = task;
    this.taskUpdate = taskUpdate;
  }

  @ApiModelProperty(required=false, value="The task this webhook refers to.")
  public SingularityTask getTask() {
    return task;
  }

  @ApiModelProperty(required=false, value="The task history update this webhook refers to.")
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
