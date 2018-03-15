package com.hubspot.mesos.protos;

import java.util.Map;

import javax.annotation.Nullable;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.hubspot.singularity.annotations.SingularityStyle;

@Immutable
@SingularityStyle
public abstract class AbstractMesosTaskStatusObject {

  @Nullable
  public abstract MesosStringValue getAgentId();

  public boolean hasAgentId() {
    return getAgentId() != null;
  }

  @Nullable
  public abstract MesosStringValue getSlaveId();

  public boolean hasSlaveId() {
    return getSlaveId() != null;
  }

  @Nullable
  public abstract Boolean getHealthy();

  public boolean hasHealthy() {
    return getHealthy() != null;
  }

  @Nullable
  public abstract String getMessage();

  public boolean hasMessage() {
    return getMessage() != null;
  }

  @Nullable
  public abstract MesosTaskStatusReason getReason();

  public boolean hasReason() {
    return getReason() != null;
  }

  @Nullable
  public abstract MesosTaskState getState();

  public boolean hasState() {
    return getState() != null;
  }

  @Nullable
  public abstract MesosStringValue getTaskId();

  public boolean hasTaskId() {
    return getTaskId() != null;
  }

  @Nullable
  public abstract Double getTimestamp();

  public boolean hasTimestamp() {
    return getTimestamp() != null;
  }

  // Unknown fields
  @JsonAnyGetter
  public abstract Map<String, Object> getAllOtherFields();
}
