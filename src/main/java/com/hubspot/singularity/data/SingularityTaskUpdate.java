package com.hubspot.singularity.data;

import org.apache.mesos.Protos.TaskState;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SingularityTaskUpdate {

  private final SingularityTask task;
  private final TaskState state;

  @JsonCreator
  public SingularityTaskUpdate(@JsonProperty("task") SingularityTask task, @JsonProperty("state") TaskState state) {
    this.task = task;
    this.state = state;
  }

  public SingularityTask getTask() {
    return task;
  }

  public TaskState getState() {
    return state;
  }
  
  public byte[] getTaskData(ObjectMapper objectMapper) throws Exception {
    return objectMapper.writeValueAsBytes(this);
  }

  public static SingularityTaskUpdate getTaskUpdateFromData(byte[] data, ObjectMapper objectMapper) throws Exception {
    return objectMapper.readValue(data, SingularityTaskUpdate.class);
  }
  
}
