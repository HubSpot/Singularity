package com.hubspot.mesos.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MesosSlaveFrameworkObject {
  
  private final List<MesosExecutorObject> executors;
  private final List<MesosExecutorObject> completedExecutors;
  
  @JsonCreator
  public MesosSlaveFrameworkObject(@JsonProperty("executors") List<MesosExecutorObject> executors, @JsonProperty("completed_executors") List<MesosExecutorObject> completedExecutors) {
    this.executors = executors;
    this.completedExecutors = completedExecutors;
  }
  
  public List<MesosExecutorObject> getCompletedExecutors() {
    return completedExecutors;
  }
  
  public List<MesosExecutorObject> getExecutors() {
    return executors;
  }
  
}
