package com.hubspot.mesos.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MesosTaskObject {

  private final MesosResourcesObject resources;
  private final String state;
  private final String id;
  private final String name;
  private final String slaveId;

  @JsonCreator
  public MesosTaskObject(@JsonProperty("resources") MesosResourcesObject resources, @JsonProperty("state") String state, @JsonProperty("id") String id, @JsonProperty("name") String name, @JsonProperty("slave_id") String slaveId) {
    this.resources = resources;
    this.state = state;
    this.id = id;
    this.name = name;
    this.slaveId = slaveId;
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

}
