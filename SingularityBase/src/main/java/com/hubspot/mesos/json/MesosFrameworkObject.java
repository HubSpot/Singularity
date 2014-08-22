package com.hubspot.mesos.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MesosFrameworkObject {

  private final String name;
  private final String id;
  private final long registeredTime;
  private final Object active;
  private final MesosResourcesObject resources;
  private final List<MesosTaskObject> tasks;

  @JsonCreator
  public MesosFrameworkObject(@JsonProperty("name") String name, @JsonProperty("id") String id, @JsonProperty("registered_time") long registeredTime, @JsonProperty("active") Object active,
      @JsonProperty("resources") MesosResourcesObject resources, @JsonProperty("tasks") List<MesosTaskObject> tasks) {
    this.name = name;
    this.id = id;
    this.registeredTime = registeredTime;
    this.resources = resources;
    this.tasks = tasks;
    this.active = active;
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

  public Object getActive() {
    return active;
  }

  @JsonIgnore
  public boolean isActive() {
    // hack to get around json changes in 0.19.1
    if (active instanceof Integer) {
      return (Integer) active > 0;
    } else {
      return Boolean.parseBoolean(active.toString());
    }

  }

}
