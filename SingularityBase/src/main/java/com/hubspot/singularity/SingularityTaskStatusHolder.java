package com.hubspot.singularity;

import java.io.IOException;

import org.apache.mesos.Protos.TaskStatus;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

public class SingularityTaskStatusHolder extends SingularityJsonObject {

  private final Optional<TaskStatus> taskStatus;
  private final SingularityTaskId taskId;
  private final long serverTimestamp;
  private final String serverId;

  public static SingularityTaskStatusHolder fromBytes(byte[] bytes, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(bytes, SingularityTaskStatusHolder.class);
    } catch (IOException e) {
      throw new SingularityJsonException(e);
    }
  }

  @JsonCreator
  public SingularityTaskStatusHolder(@JsonProperty("taskId") SingularityTaskId taskId, @JsonProperty("taskStatus") Optional<TaskStatus> taskStatus, @JsonProperty("serverTimestamp") long serverTimestamp, @JsonProperty("serverId") String serverId) {
    this.taskId = taskId;
    this.taskStatus = taskStatus;
    this.serverTimestamp = serverTimestamp;
    this.serverId = serverId;
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

  @Override
  public String toString() {
    return "SingularityTaskStatusHolder [taskStatus=" + taskStatus + ", taskId=" + taskId + ", serverTimestamp=" + serverTimestamp + ", serverId=" + serverId + "]";
  }

}
