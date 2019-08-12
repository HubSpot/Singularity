package com.hubspot.mesos.protos;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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
    this.agentId = agentId.isPresent() ? agentId : slaveId;
    this.slaveId = agentId.isPresent() ? agentId : slaveId;
    this.healthy = healthy;
    this.message = message;
    this.reason = reason;
    this.state = state;
    this.taskId = taskId;
    this.timestamp = timestamp;
    this.allOtherFields = new HashMap<>();
  }

  public MesosStringValue getAgentId() {
    return agentId.orElse(null);
  }

  public boolean hasAgentId() {
    return agentId.isPresent();
  }

  public MesosStringValue getSlaveId() {
    return slaveId.orElse(null);
  }

  public boolean hasSlaveId() {
    return slaveId.isPresent();
  }

  public Boolean getHealthy() {
    return healthy.orElse(null);
  }

  public boolean hasHealthy() {
    return healthy.isPresent();
  }

  public String getMessage() {
    return message.orElse(null);
  }

  public boolean hasMessage() {
    return message.isPresent();
  }

  public MesosTaskStatusReason getReason() {
    return reason.orElse(null);
  }

  public boolean hasReason() {
    return reason.isPresent();
  }

  public MesosTaskState getState() {
    return state.orElse(null);
  }

  public boolean hasState() {
    return state.isPresent();
  }

  public MesosStringValue getTaskId() {
    return taskId.orElse(null);
  }

  public boolean hasTaskId() {
    return taskId.isPresent();
  }

  public Double getTimestamp() {
    return timestamp.orElse(null);
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
