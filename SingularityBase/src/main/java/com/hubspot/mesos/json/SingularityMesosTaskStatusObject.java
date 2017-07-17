package com.hubspot.mesos.json;

import org.apache.mesos.v1.Protos.AgentID;
import org.apache.mesos.v1.Protos.ContainerStatus;
import org.apache.mesos.v1.Protos.ExecutorID;
import org.apache.mesos.v1.Protos.Labels;
import org.apache.mesos.v1.Protos.TaskID;
import org.apache.mesos.v1.Protos.TaskState;
import org.apache.mesos.v1.Protos.TaskStatus;
import org.apache.mesos.v1.Protos.TaskStatus.Reason;
import org.apache.mesos.v1.Protos.TaskStatus.Source;
import org.apache.mesos.v1.Protos.TimeInfo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityMesosTaskStatusObject {
  private final Optional<AgentID> agentId;
  private final Optional<AgentID> slaveId;
  private final Optional<ContainerStatus> containerStatus;
  private final Optional<ExecutorID> executorId;
  private final Optional<Boolean> healthy;
  private final Optional<Labels> labels;
  private final Optional<String> message;
  private final Optional<Reason> reason;
  private final Optional<Source> source;
  private final Optional<TaskState> state;
  private final Optional<TaskID> taskId;
  private final Optional<Double> timestamp;
  private final Optional<TimeInfo> unreachableTime;

  @JsonCreator
  public SingularityMesosTaskStatusObject(@JsonProperty("agentId") Optional<AgentID> agentId,
                                          @JsonProperty("slaveId") Optional<AgentID> slaveId,
                                          @JsonProperty("containerStatus") Optional<ContainerStatus> containerStatus,
                                          @JsonProperty("executorID") Optional<ExecutorID> executorId,
                                          @JsonProperty("healthy") Optional<Boolean> healthy,
                                          @JsonProperty("labels") Optional<Labels> labels,
                                          @JsonProperty("message") Optional<String> message,
                                          @JsonProperty("reason") Optional<Reason> reason,
                                          @JsonProperty("source") Optional<Source> source,
                                          @JsonProperty("state") Optional<TaskState> state,
                                          @JsonProperty("taskId") Optional<TaskID> taskId,
                                          @JsonProperty("timestamp") Optional<Double> timestamp,
                                          @JsonProperty("unreachableTime") Optional<TimeInfo> unreachableTime) {
    this.agentId = agentId.or(slaveId);
    this.slaveId = agentId.or(slaveId);
    this.containerStatus = containerStatus;
    this.executorId = executorId;
    this.healthy = healthy;
    this.labels = labels;
    this.message = message;
    this.reason = reason;
    this.source = source;
    this.state = state;
    this.taskId = taskId;
    this.timestamp = timestamp;
    this.unreachableTime = unreachableTime;
  }

  public static SingularityMesosTaskStatusObject fromProtos(TaskStatus status) {
    return new SingularityMesosTaskStatusObject(
        status.hasAgentId() ? Optional.of(status.getAgentId()) : Optional.absent(),
        status.hasAgentId() ? Optional.of(status.getAgentId()) : Optional.absent(),
        status.hasContainerStatus() ? Optional.of(status.getContainerStatus()) : Optional.absent(),
        status.hasExecutorId() ? Optional.of(status.getExecutorId()) : Optional.absent(),
        status.hasHealthy() ? Optional.of(status.getHealthy()) : Optional.absent(),
        status.hasLabels() ? Optional.of(status.getLabels()) : Optional.absent(),
        status.hasMessage() ? Optional.of(status.getMessage()) : Optional.absent(),
        status.hasReason() ? Optional.of(status.getReason()) : Optional.absent(),
        status.hasSource() ? Optional.of(status.getSource()) : Optional.absent(),
        status.hasState() ? Optional.of(status.getState()) : Optional.absent(),
        status.hasTaskId() ? Optional.of(status.getTaskId()) : Optional.absent(),
        status.hasTimestamp() ? Optional.of(status.getTimestamp()) : Optional.absent(),
        status.hasUnreachableTime() ? Optional.of(status.getUnreachableTime()) : Optional.absent()
    );
  }

  public AgentID getAgentId() {
    return agentId.orNull();
  }

  public boolean hasAgentId() {
    return agentId.isPresent();
  }

  public AgentID getSlaveId() {
    return slaveId.orNull();
  }
  public boolean hasSlaveId() {
    return slaveId.isPresent();
  }

  public ContainerStatus getContainerStatus() {
    return containerStatus.orNull();
  }

  public boolean hasContainerStatus() {
    return containerStatus.isPresent();
  }

  public ExecutorID getExecutorId() {
    return executorId.orNull();
  }

  public boolean hasExecutorId() {
    return executorId.isPresent();
  }

  public Boolean getHealthy() {
    return healthy.orNull();
  }

  public boolean hasHealthy() {
    return healthy.isPresent();
  }

  public Labels getLabels() {
    return labels.orNull();
  }

  public boolean hasLabels() {
    return labels.isPresent();
  }

  public String getMessage() {
    return message.orNull();
  }

  public boolean hasMessage() {
    return message.isPresent();
  }

  public Reason getReason() {
    return reason.orNull();
  }

  public boolean hasReason() {
    return reason.isPresent();
  }

  public Source getSource() {
    return source.orNull();
  }

  public boolean hasSource() {
    return source.isPresent();
  }

  public TaskState getState() {
    return state.orNull();
  }

  public boolean hasState() {
    return state.isPresent();
  }

  public TaskID getTaskId() {
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

  public TimeInfo getUnreachableTime() {
    return unreachableTime.orNull();
  }

  public boolean hasUnreachableTime() {
    return unreachableTime.isPresent();
  }

  @Override
  public String toString() {
    return "SingularityMesosTaskStatusObject{" +
        "agentId=" + agentId +
        ", slaveId=" + slaveId +
        ", containerStatus=" + containerStatus +
        ", executorId=" + executorId +
        ", healthy=" + healthy +
        ", labels=" + labels +
        ", message=" + message +
        ", reason=" + reason +
        ", source=" + source +
        ", state=" + state +
        ", taskId=" + taskId +
        ", timestamp=" + timestamp +
        ", unreachableTime=" + unreachableTime +
        '}';
  }
}
