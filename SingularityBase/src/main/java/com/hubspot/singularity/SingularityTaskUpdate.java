package com.hubspot.singularity;

import org.apache.mesos.Protos.TaskState;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SingularityTaskUpdate extends SingularityJsonObject {

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
  
  public static SingularityTaskUpdate fromBytes(byte[] bytes, ObjectMapper objectMapper) throws Exception {
    return objectMapper.readValue(bytes, SingularityTaskUpdate.class);
  }

  @Override
  public String toString() {
    return "SingularityTaskUpdate [task=" + task + ", state=" + state + "]";
  }
  
}
