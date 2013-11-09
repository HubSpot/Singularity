package com.hubspot.mesos.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MesosSlaveStateObject {

//  {
//    attributes: { },
//    hostname: "dahlbero.iad01.hubspot-networks.net",
//    id: "201309092300-1947527690-5050-13008-3",
//    pid: "slave(1)@10.238.161.84:5051",
//    registered_time: 1378767835,
//    resources: {
//    cpus: 4,
//    disk: 396445,
//    mem: 13987,
//    ports: "[31000-32000]"
//    }
//    
  
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
