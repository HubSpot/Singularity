package com.hubspot.mesos.json;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MesosMasterSlaveObject {

  private final String id;
  private final String pid;
  private final String hostname;
  private final Map<String, String> attributes;
  private final long registeredTime;
  private final MesosResourcesObject resources;

  @JsonCreator
  public MesosMasterSlaveObject(@JsonProperty("id") String id, @JsonProperty("pid") String pid, @JsonProperty("hostname") String hostname, @JsonProperty("registered_time") long registeredTime,
      @JsonProperty("resources") MesosResourcesObject resources, @JsonProperty("attributes") Map<String, String> attributes) {
    this.id = id;
    this.pid = pid;
    this.hostname = hostname;
    this.registeredTime = registeredTime;
    this.resources = resources;
    this.attributes = attributes;
  }

  public Map<String, String> getAttributes() {
    return attributes;
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

  public long getRegisteredTime() {
    return registeredTime;
  }

  public MesosResourcesObject getResources() {
    return resources;
  }

}
