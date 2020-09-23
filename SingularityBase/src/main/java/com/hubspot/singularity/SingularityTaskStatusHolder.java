package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.mesos.protos.MesosTaskStatusObject;
import java.util.Optional;

public class SingularityTaskStatusHolder {
  private final Optional<MesosTaskStatusObject> taskStatus;
  private final SingularityTaskId taskId;
  private final long serverTimestamp;
  private final String serverId;
  private final Optional<String> agentId;

  public SingularityTaskStatusHolder(
    SingularityTaskId taskId,
    Optional<MesosTaskStatusObject> taskStatus,
    long serverTimestamp,
    String serverId,
    Optional<String> agentId
  ) {
    this(taskId, taskStatus, serverTimestamp, serverId, Optional.empty(), agentId);
  }

  @JsonCreator
  public SingularityTaskStatusHolder(
    @JsonProperty("taskId") SingularityTaskId taskId,
    @JsonProperty("taskStatus") Optional<MesosTaskStatusObject> taskStatus,
    @JsonProperty("serverTimestamp") long serverTimestamp,
    @JsonProperty("serverId") String serverId,
    @JsonProperty("slaveId") Optional<String> slaveId,
    @JsonProperty("agentId") Optional<String> agentId
  ) {
    this.taskId = taskId;
    this.taskStatus = taskStatus;
    this.serverTimestamp = serverTimestamp;
    this.serverId = serverId;
    this.agentId = agentId.isPresent() ? agentId : slaveId;
  }

  public Optional<MesosTaskStatusObject> getTaskStatus() {
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

  public Optional<String> getAgentId() {
    return agentId;
  }

  @Deprecated
  public Optional<String> getSlaveId() {
    return agentId;
  }

  @Override
  public String toString() {
    return (
      "SingularityTaskStatusHolder{" +
      "taskStatus=" +
      taskStatus +
      ", taskId=" +
      taskId +
      ", serverTimestamp=" +
      serverTimestamp +
      ", serverId='" +
      serverId +
      '\'' +
      ", agentId=" +
      agentId +
      '}'
    );
  }
}
