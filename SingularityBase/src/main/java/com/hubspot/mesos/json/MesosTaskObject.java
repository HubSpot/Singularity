package com.hubspot.mesos.json;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MesosTaskObject {

  private final MesosResourcesObject resources;
  private final String state;
  private final String id;
  private final String name;
  private final String slaveId;
  private final String frameworkId;
  private final String executorId;

  @JsonCreator
  public MesosTaskObject(@JsonProperty("resources") MesosResourcesObject resources, @JsonProperty("state") String state, @JsonProperty("id") String id, @JsonProperty("name") String name, @JsonProperty("slave_id") String slaveId,
    @JsonProperty("framework_id") String frameworkId, @JsonProperty("executor_id") String executorId) {
    this.resources = resources;
    this.state = state;
    this.id = id;
    this.name = name;
    this.slaveId = slaveId;
    this.frameworkId = frameworkId;
    this.executorId = executorId;
  }

  public String getSlaveId() {
    return slaveId;
  }

  public MesosResourcesObject getResources() {
    return resources;
  }

  public String getState() {
    return state;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getFrameworkId() {
    return frameworkId;
  }

  public String getExecutorId() {
    return executorId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MesosTaskObject that = (MesosTaskObject) o;
    return Objects.equals(resources, that.resources) &&
        Objects.equals(state, that.state) &&
        Objects.equals(id, that.id) &&
        Objects.equals(name, that.name) &&
        Objects.equals(slaveId, that.slaveId) &&
        Objects.equals(frameworkId, that.frameworkId) &&
        Objects.equals(executorId, that.executorId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(resources, state, id, name, slaveId, frameworkId, executorId);
  }

  @Override
  public String toString() {
    return "MesosTaskObject{" +
        "resources=" + resources +
        ", state='" + state + '\'' +
        ", id='" + id + '\'' +
        ", name='" + name + '\'' +
        ", slaveId='" + slaveId + '\'' +
        ", frameworkId='" + frameworkId + '\'' +
        ", executorId='" + executorId + '\'' +
        '}';
  }
}
