package com.hubspot.mesos.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MesosMasterStateObject {

  private final int activatedSlaves;
  private final int deactivatedSlaves;

  private final int failedTasks;
  private final int killedTasks;
  private final int startedTasks;
  private final int stagedTasks;

  private final String leader;
  private final String pid;

  private final long startTime;

  private final List<MesosMasterSlaveObject> slaves;
  private final List<MesosFrameworkObject> frameworks;

  @JsonCreator
  public MesosMasterStateObject(@JsonProperty("activated_slaves") int activatedSlaves, @JsonProperty("deactivated_slaves") int deactivatedSlaves, @JsonProperty("failed_tasks") int failedTasks,
      @JsonProperty("killed_tasks") int killedTasks, @JsonProperty("started_tasks") int startedTasks, @JsonProperty("staged_tasks") int stagedTasks,
      @JsonProperty("leader") String leader, @JsonProperty("pid") String pid, @JsonProperty("start_time") long startTime,
      @JsonProperty("slaves") List<MesosMasterSlaveObject> slaves, @JsonProperty("frameworks") List<MesosFrameworkObject> frameworks) {
    this.activatedSlaves = activatedSlaves;
    this.deactivatedSlaves = deactivatedSlaves;
    this.failedTasks = failedTasks;
    this.killedTasks = killedTasks;
    this.startedTasks = startedTasks;
    this.stagedTasks = stagedTasks;
    this.leader = leader;
    this.pid = pid;
    this.startTime = startTime;
    this.slaves = slaves;
    this.frameworks = frameworks;
  }

  public int getActivatedSlaves() {
    return activatedSlaves;
  }

  public int getDeactivatedSlaves() {
    return deactivatedSlaves;
  }

  public int getFailedTasks() {
    return failedTasks;
  }

  public int getKilledTasks() {
    return killedTasks;
  }

  public int getStartedTasks() {
    return startedTasks;
  }

  public int getStagedTasks() {
    return stagedTasks;
  }

  public String getLeader() {
    return leader;
  }

  public String getPid() {
    return pid;
  }

  public long getStartTime() {
    return startTime;
  }

  public List<MesosMasterSlaveObject> getSlaves() {
    return slaves;
  }

  public List<MesosFrameworkObject> getFrameworks() {
    return frameworks;
  }

}
