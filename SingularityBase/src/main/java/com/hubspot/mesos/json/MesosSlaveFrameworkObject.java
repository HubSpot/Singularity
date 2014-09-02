package com.hubspot.mesos.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MesosSlaveFrameworkObject {

  private final List<MesosExecutorObject> executors;
  private final List<MesosExecutorObject> completedExecutors;
  private final String id;

  @JsonCreator
  public MesosSlaveFrameworkObject(@JsonProperty("id") String id, @JsonProperty("executors") List<MesosExecutorObject> executors, @JsonProperty("completed_executors") List<MesosExecutorObject> completedExecutors) {
    this.id = id;
    this.executors = executors;
    this.completedExecutors = completedExecutors;
  }

  public String getId() {
    return id;
  }

  public List<MesosExecutorObject> getCompletedExecutors() {
    return completedExecutors;
  }

  public List<MesosExecutorObject> getExecutors() {
    return executors;
  }

}
