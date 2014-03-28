package com.hubspot.singularity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SingularityTaskUpdate extends SingularityJsonObject {

  private final List<SingularityTask> healthyTasks;
  private final List<SingularityTask> unhealthyTasks;
  
  public static SingularityTaskUpdate fromBytes(byte[] bytes, ObjectMapper objectMapper) throws Exception {
    return objectMapper.readValue(bytes, SingularityTaskUpdate.class);
  }

  @JsonCreator
  public SingularityTaskUpdate(@JsonProperty("healthyTasks") List<SingularityTask> healthyTasks, @JsonProperty("unhealthyTasks") List<SingularityTask> unhealthyTasks) {
    this.healthyTasks = healthyTasks;
    this.unhealthyTasks = unhealthyTasks;
  }

  public List<SingularityTask> getHealthyTasks() {
    return healthyTasks;
  }

  public List<SingularityTask> getUnhealthyTasks() {
    return unhealthyTasks;
  }

  @Override
  public String toString() {
    return "SingularityTaskUpdate [healthyTasks=" + healthyTasks + ", unhealthyTasks=" + unhealthyTasks + "]";
  }
  
}
