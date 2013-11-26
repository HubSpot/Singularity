package com.hubspot.mesos.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MesosSlaveStateObject {

  private final String id;
  private final String pid;
  private final String hostname;
  
  private final long startTime;
  
  private final MesosResourcesObject resources;

  private final List<MesosSlaveFrameworkObject> frameworks;

  @JsonCreator
  public MesosSlaveStateObject(@JsonProperty("id") String id, @JsonProperty("pid") String pid, @JsonProperty("hostname") String hostname, @JsonProperty("start_time") long startTime, @JsonProperty("resources") MesosResourcesObject resources, @JsonProperty("frameworks")  List<MesosSlaveFrameworkObject> frameworks) {
    this.id = id;
    this.pid = pid;
    this.hostname = hostname;
    this.startTime = startTime;
    this.resources = resources;
    this.frameworks = frameworks;
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
  
  public List<MesosSlaveFrameworkObject> getFrameworks() {
    return frameworks;
  }

  public long getStartTime() {
    return startTime;
  }

  public MesosResourcesObject getResources() {
    return resources;
  }
  
}
