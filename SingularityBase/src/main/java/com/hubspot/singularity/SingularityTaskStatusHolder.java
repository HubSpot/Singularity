package com.hubspot.singularity;

import org.apache.mesos.Protos.TaskStatus;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityTaskStatusHolder {

  private final Optional<TaskStatus> taskStatus;
  private final SingularityTaskId taskId;
  private final long serverTimestamp;
  private final String serverId;
  private final Optional<String> slaveId;

  @JsonCreator
  public SingularityTaskStatusHolder(@JsonProperty("taskId") SingularityTaskId taskId, @JsonProperty("taskStatus") Optional<TaskStatus> taskStatus, @JsonProperty("serverTimestamp") long serverTimestamp, @JsonProperty("serverId") String serverId, @JsonProperty("slaveId") Optional<String> slaveId) {
    this.taskId = taskId;
    this.taskStatus = taskStatus;
    this.serverTimestamp = serverTimestamp;
    this.serverId = serverId;
    this.slaveId = slaveId;
  }

  public Optional<TaskStatus> getTaskStatus() {
    return taskStatus;
  }

  public SingularityTaskId getTaskId() {
    return taskId;
  }

  public long getServerTimestamp() {
    return serverTimestamp;
  }

  public String getServerId() {
    return serverId;
  }

  public Optional<String> getSlaveId() {
    return slaveId;
  }

  @Override
  public String toString() {
    return "SingularityTaskStatusHolder [taskStatus=" + taskStatus + ", taskId=" + taskId + ", serverTimestamp=" + serverTimestamp + ", serverId=" + serverId + ", slaveId=" + slaveId + "]";
  }

}
