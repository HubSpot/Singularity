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
  private final int failedTasks;
  private final int killedTasks;
  private final int stagedTasks;

  @JsonCreator
  public MesosSlaveStateObject(@JsonProperty("id") String id, @JsonProperty("pid") String pid,
                               @JsonProperty("hostname") String hostname, @JsonProperty("start_time") long startTime,
                               @JsonProperty("resources") MesosResourcesObject resources,
                               @JsonProperty("frameworks")  List<MesosSlaveFrameworkObject> frameworks,
                               @JsonProperty("finished_tasks") int finishedTasks,
                               @JsonProperty("lost_tasks") int lostTasks,
                               @JsonProperty("started_tasks") int startedTasks,
                               @JsonProperty("failed_tasks") int failedTasks,
                               @JsonProperty("killed_tasks") int killedTasks,
                               @JsonProperty("staged_tasks") int stagedTasks) {
    this.id = id;
    this.pid = pid;
    this.hostname = hostname;
    this.startTime = startTime;
    this.resources = resources;
    this.frameworks = frameworks;

    this.finishedTasks = finishedTasks;
    this.lostTasks = lostTasks;
    this.startedTasks = startedTasks;
    this.failedTasks = failedTasks;
    this.killedTasks = killedTasks;
    this.stagedTasks = stagedTasks;
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

  public int getFailedTasks() {
    return failedTasks;
  }

  public int getKilledTasks() {
    return killedTasks;
  }

  public int getStagedTasks() {
    return stagedTasks;
  }
}
