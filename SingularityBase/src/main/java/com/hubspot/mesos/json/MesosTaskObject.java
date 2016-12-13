package com.hubspot.mesos.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.hubspot.mesos.SingularityMesosTaskLabel;

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
  public String toString() {
    return Objects.toStringHelper(this)
      .add("resources", resources)
      .add("state", state)
      .add("id", id)
      .add("name", name)
      .add("slaveId", slaveId)
      .add("frameworkId", frameworkId)
      .add("executorId", executorId)
      .toString();
  }
}
