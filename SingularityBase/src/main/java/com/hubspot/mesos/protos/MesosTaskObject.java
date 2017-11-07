package com.hubspot.mesos.protos;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

/*
 * Mimics the TaskInfo object from mesos, with the addition that we can read
 * AgentID from either a field named slaveId or a field named agentId for
 * better backwards compatibility
 */
public class MesosTaskObject {
  private final MesosStringValue taskId;
  private final Optional<MesosExecutorInfo> executor;
  private final MesosLabels labels;
  private final MesosStringValue agentId;
  private final MesosStringValue slaveId;
  private final List<MesosResourceObject> resources;
  private final String name;
  private final Map<String, Object> allOtherFields;

  @JsonCreator
  public MesosTaskObject(@JsonProperty("taskId") MesosStringValue taskId,
                         @JsonProperty("executor") Optional<MesosExecutorInfo> executor,
                         @JsonProperty("labels") MesosLabels labels,
                         @JsonProperty("agentId") MesosStringValue agentId,
                         @JsonProperty("slaveId") MesosStringValue slaveId,
                         @JsonProperty("resources") List<MesosResourceObject> resources,
                         @JsonProperty("name") String name) {
    this.taskId = taskId;
    this.executor = executor;
    this.labels = labels;
    this.agentId = agentId != null ? agentId : slaveId;
    this.slaveId = agentId != null ? agentId : slaveId;
    this.resources = resources != null ? resources : Collections.emptyList();
    this.name = name;
    this.allOtherFields = new HashMap<>();
  }

  public MesosStringValue getTaskId() {
    return taskId;
  }

  public MesosExecutorInfo getExecutor() {
    return executor.orNull();
  }

  public boolean hasExecutor() {
    return executor.isPresent();
  }

  public MesosLabels getLabels() {
    return labels;
  }

  public MesosStringValue getAgentId() {
    return agentId;
  }

  public MesosStringValue getSlaveId() {
    return slaveId;
  }

  public List<MesosResourceObject> getResources() {
    return resources;
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

  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof MesosTaskObject) {
      final MesosTaskObject that = (MesosTaskObject) obj;
      return Objects.equals(this.taskId, that.taskId) &&
          Objects.equals(this.executor, that.executor) &&
          Objects.equals(this.labels, that.labels) &&
          Objects.equals(this.agentId, that.agentId) &&
          Objects.equals(this.slaveId, that.slaveId) &&
          Objects.equals(this.resources, that.resources) &&
          Objects.equals(this.name, that.name) &&
          Objects.equals(this.allOtherFields, that.allOtherFields);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(taskId, executor, labels, agentId, slaveId, resources, name, allOtherFields);
  }

  @Override
  public String toString() {
    return "MesosTaskObject{" +
        "taskId=" + taskId +
        ", executor=" + executor +
        ", labels=" + labels +
        ", agentId=" + agentId +
        ", slaveId=" + slaveId +
        ", resources=" + resources +
        ", name='" + name + '\'' +
        ", allOtherFields=" + allOtherFields +
        '}';
  }
}
