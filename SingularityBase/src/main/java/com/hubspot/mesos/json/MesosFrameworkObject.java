package com.hubspot.mesos.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MesosFrameworkObject {

  private final String name;
  private final String id;
  private final String pid;
  private final String hostname;
  private final String webuiUrl;
  private final String user;
  private final String role;
  private final long registeredTime;
  private final long unregisteredTime;
  private final long reregisteredTime;
  private final boolean active;
  private final boolean checkpoint;
  private final MesosResourcesObject resources;
  private final MesosResourcesObject usedResources;
  private final MesosResourcesObject offeredResources;
  private final List<MesosTaskObject> tasks;

  @JsonCreator
  public MesosFrameworkObject(@JsonProperty("name") String name, @JsonProperty("id") String id, @JsonProperty("pid") String pid, @JsonProperty("hostname") String hostname, @JsonProperty("webui_url") String webuiUrl,
      @JsonProperty("user") String user, @JsonProperty("role") String role, @JsonProperty("registered_time") long registeredTime, @JsonProperty("unregistered_time") long unregisteredTime, @JsonProperty("reregistered_time") long reregisteredTime,
      @JsonProperty("active") boolean active, @JsonProperty("checkpoint") boolean checkpoint, @JsonProperty("resources") MesosResourcesObject resources, @JsonProperty("used_resources") MesosResourcesObject usedResources,
      @JsonProperty("offered_resources") MesosResourcesObject offeredResources, @JsonProperty("tasks") List<MesosTaskObject> tasks) {
    this.name = name;
    this.id = id;
    this.pid = pid;
    this.hostname = hostname;
    this.webuiUrl = webuiUrl;
    this.user = user;
    this.role = role;
    this.checkpoint = checkpoint;
    this.registeredTime = registeredTime;
    this.unregisteredTime = unregisteredTime;
    this.reregisteredTime = reregisteredTime;
    this.resources = resources;
    this.usedResources = usedResources;
    this.offeredResources = offeredResources;
    this.tasks = tasks;
    this.active = active;
  }

  public String getName() {
    return name;
  }

  public String getId() {
    return id;
  }

  public String getPid() {
    return pid;
  }

  public String getHostname() {
    return hostname;
  }

  public String getWebuiUrl() {
    return webuiUrl;
  }

  public String getUser() {
    return user;
  }

  public String getRole() {
    return role;
  }

  public long getRegisteredTime() {
    return registeredTime;
  }

  public long getUnregisteredTime() {
    return unregisteredTime;
  }

  public long getReregisteredTime() {
    return reregisteredTime;
  }

  public boolean isActive() {
    return active;
  }

  public boolean isCheckpoint() {
    return checkpoint;
  }

  public MesosResourcesObject getResources() {
    return resources;
  }

  public MesosResourcesObject getUsedResources() {
    return usedResources;
  }

  public MesosResourcesObject getOfferedResources() {
    return offeredResources;
  }

  public List<MesosTaskObject> getTasks() {
    return tasks;
  }
}
