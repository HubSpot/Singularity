package com.hubspot.mesos.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MesosSlaveStateObject {

  private final String id;
  private final String pid;
  private final String hostname;
  
  private final long startTime;
  
  private final MesosResourcesObject resources;

  private final List<MesosSlaveFrameworkObject> frameworks;

  private final int finishedTasks;
  private final int lostTasks;
  private final int startedTasks;

  @JsonCreator
  public MesosSlaveStateObject(@JsonProperty("id") String id, @JsonProperty("pid") String pid, @JsonProperty("hostname") String hostname, @JsonProperty("start_time") long startTime, @JsonProperty("resources") MesosResourcesObject resources, @JsonProperty("frameworks")  List<MesosSlaveFrameworkObject> frameworks, @JsonProperty("finishedTasks") int finishedTasks, @JsonProperty("lostTasks") int lostTasks, @JsonProperty("startedTasks") int startedTasks) {
    this.id = id;
    this.pid = pid;
    this.hostname = hostname;
    this.startTime = startTime;
    this.resources = resources;
    this.frameworks = frameworks;
    this.finishedTasks = finishedTasks;
    this.lostTasks = lostTasks;
    this.startedTasks = startedTasks;
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

  public int getFinishedTasks() {
    return finishedTasks;
  }

  public int getLostTasks() {
    return lostTasks;
  }

  public int getStartedTasks() {
    return startedTasks;
  }
}
