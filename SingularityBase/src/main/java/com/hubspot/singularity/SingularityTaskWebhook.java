package com.hubspot.singularity;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SingularityTaskWebhook extends SingularityJsonObject {

  private final SingularityTask task;
  private final SingularityTaskHistoryUpdate taskUpdate;

  public static SingularityTaskWebhook fromBytes(byte[] bytes, ObjectMapper objectMapper) throws SingularityJsonException {
    try {
      return objectMapper.readValue(bytes, SingularityTaskWebhook.class);
    } catch (IOException e) {
      throw new SingularityJsonException(e);
    }
  }

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
