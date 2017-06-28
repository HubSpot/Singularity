package com.hubspot.mesos.json;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MesosFrameworkObject that = (MesosFrameworkObject) o;
    return registeredTime == that.registeredTime &&
        unregisteredTime == that.unregisteredTime &&
        reregisteredTime == that.reregisteredTime &&
        active == that.active &&
        checkpoint == that.checkpoint &&
        Objects.equals(name, that.name) &&
        Objects.equals(id, that.id) &&
        Objects.equals(pid, that.pid) &&
        Objects.equals(hostname, that.hostname) &&
        Objects.equals(webuiUrl, that.webuiUrl) &&
        Objects.equals(user, that.user) &&
        Objects.equals(role, that.role) &&
        Objects.equals(resources, that.resources) &&
        Objects.equals(usedResources, that.usedResources) &&
        Objects.equals(offeredResources, that.offeredResources) &&
        Objects.equals(tasks, that.tasks);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, id, pid, hostname, webuiUrl, user, role, registeredTime, unregisteredTime, reregisteredTime, active, checkpoint, resources, usedResources, offeredResources, tasks);
  }

  @Override
  public String toString() {
    return "MesosFrameworkObject{" +
        "name='" + name + '\'' +
        ", id='" + id + '\'' +
        ", pid='" + pid + '\'' +
        ", hostname='" + hostname + '\'' +
        ", webuiUrl='" + webuiUrl + '\'' +
        ", user='" + user + '\'' +
        ", role='" + role + '\'' +
        ", registeredTime=" + registeredTime +
        ", unregisteredTime=" + unregisteredTime +
        ", reregisteredTime=" + reregisteredTime +
        ", active=" + active +
        ", checkpoint=" + checkpoint +
        ", resources=" + resources +
        ", usedResources=" + usedResources +
        ", offeredResources=" + offeredResources +
        ", tasks=" + tasks +
        '}';
  }
}
