package com.hubspot.mesos.protos;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class MesosTaskStatusObject {
  private final Optional<MesosStringValue> agentId;
  private final Optional<MesosStringValue> slaveId;
  private final Optional<Boolean> healthy;
  private final Optional<String> message;
  private final Optional<MesosTaskStatusReason> reason;
  private final Optional<MesosTaskState> state;
  private final Optional<MesosStringValue> taskId;
  private final Optional<Double> timestamp;
  private final Map<String, Object> allOtherFields;

  @JsonCreator
  public MesosTaskStatusObject(@JsonProperty("agentId") Optional<MesosStringValue> agentId,
                               @JsonProperty("slaveId") Optional<MesosStringValue> slaveId,
                               @JsonProperty("healthy") Optional<Boolean> healthy,
                               @JsonProperty("message") Optional<String> message,
                               @JsonProperty("reason") Optional<MesosTaskStatusReason> reason,
                               @JsonProperty("state") Optional<MesosTaskState> state,
                               @JsonProperty("taskId") Optional<MesosStringValue> taskId,
                               @JsonProperty("timestamp") Optional<Double> timestamp) {
    this.agentId = agentId.or(slaveId);
    this.slaveId = agentId.or(slaveId);
    this.healthy = healthy;
    this.message = message;
    this.reason = reason;
    this.state = state;
    this.taskId = taskId;
    this.timestamp = timestamp;
    this.allOtherFields = new HashMap<>();
  }

  public MesosStringValue getAgentId() {
    return agentId.orNull();
  }

  public boolean hasAgentId() {
    return agentId.isPresent();
  }

  public MesosStringValue getSlaveId() {
    return slaveId.orNull();
  }

  public boolean hasSlaveId() {
    return slaveId.isPresent();
  }

  public Boolean getHealthy() {
    return healthy.orNull();
  }

  public boolean hasHealthy() {
    return healthy.isPresent();
  }

  public String getMessage() {
    return message.orNull();
  }

  public boolean hasMessage() {
    return message.isPresent();
  }

  public MesosTaskStatusReason getReason() {
    return reason.orNull();
  }

  public boolean hasReason() {
    return reason.isPresent();
  }

  public MesosTaskState getState() {
    return state.orNull();
  }

  public boolean hasState() {
    return state.isPresent();
  }

  public MesosStringValue getTaskId() {
    return taskId.orNull();
  }

  public boolean hasTaskId() {
    return taskId.isPresent();
  }

  public Double getTimestamp() {
    return timestamp.orNull();
  }

  public boolean hasTimestamp() {
    return timestamp.isPresent();
  }

  // Unknown fields
  @JsonAnyGetter
  public Map<String, Object> getAllOtherFields() {
    return allOtherFields;
  }

  @JsonAnySetter
  public void setAllOtherFields(String name, Object value) {
    allOtherFields.put(name, value);
  }


  @Override
  public String toString() {
    return "SingularityMesosTaskStatusObject{" +
        "agentId=" + agentId +
        ", slaveId=" + slaveId +
        ", healthy=" + healthy +
        ", message=" + message +
        ", reason=" + reason +
        ", state=" + state +
        ", taskId=" + taskId +
        ", timestamp=" + timestamp +
        '}';
  }
}
