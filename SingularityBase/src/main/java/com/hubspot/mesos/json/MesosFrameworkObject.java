package com.hubspot.mesos.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MesosFrameworkObject {
  
  private final String name;
  private final String id;
  private final long registeredTime;
  private final int active;
  private final MesosResourcesObject resources;
  private final List<MesosTaskObject> tasks;
  
  @JsonCreator
  public MesosFrameworkObject(@JsonProperty("name") String name, @JsonProperty("id") String id, @JsonProperty("registered_time") long registeredTime, @JsonProperty("active") int active, 
      @JsonProperty("resources") MesosResourcesObject resources, @JsonProperty("tasks") List<MesosTaskObject> tasks) {
    this.name = name;
    this.id = id;
    this.registeredTime = registeredTime;
    this.active = active;
    this.resources = resources;
    this.tasks = tasks;
  }
  
  public String getId() {
    return id;
  }

  public List<MesosTaskObject> getTasks() {
    return tasks;
  }
  
  public MesosResourcesObject getResources() {
    return resources;
  }
  
  public String getName() {
    return name;
  }
  
  public long getRegisteredTime() {
    return registeredTime;
  }
  
  public int getActive() {
    return active;
  }
  
}
