package com.hubspot.mesos.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MesosExecutorObject {
  
  private final String directory;
  private final String id;
  private final String name;
  private final MesosResourcesObject resources;
  private final List<MesosTaskObject> tasks;
  private final List<MesosTaskObject> completedTasks;
  
  @JsonCreator
  public MesosExecutorObject(@JsonProperty("id") String id, @JsonProperty("name") String name, @JsonProperty("directory") String directory, @JsonProperty("resources") MesosResourcesObject resources, @JsonProperty("tasks") List<MesosTaskObject> tasks,  @JsonProperty("completed_tasks") List<MesosTaskObject> completedTasks) {
    this.name = name;
    this.id = id;
    this.directory = directory;
    this.resources = resources;
    this.tasks = tasks;
    this.completedTasks = completedTasks;
  }
  
  public List<MesosTaskObject> getCompletedTasks() {
    return completedTasks;
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

  public String getDirectory() {
    return directory;
  }

  public String getId() {
    return id;
  }
  
}
