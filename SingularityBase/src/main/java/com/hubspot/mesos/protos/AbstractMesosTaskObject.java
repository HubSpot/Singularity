package com.hubspot.mesos.protos;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hubspot.singularity.annotations.SingularityStyle;

import io.swagger.v3.oas.annotations.media.Schema;

/*
 * Mimics the TaskInfo object from mesos, with the addition that we can read
 * AgentID from either a field named slaveId or a field named agentId for
 * better backwards compatibility
 */
@Immutable
@SingularityStyle
@Schema(description = "The mesos protos representation of a task")
public abstract class AbstractMesosTaskObject {

  public abstract MesosStringValue getTaskId();

  @Nullable
  public abstract MesosExecutorInfo getExecutor();

  @JsonIgnore
  public boolean hasExecutor() {
    return getExecutor() != null;
  }

  @Nullable
  public abstract MesosLabels getLabels();

  @Nullable
  public abstract MesosStringValue getAgentId();

  @Nullable
  public abstract MesosStringValue getSlaveId();

  public abstract List<MesosResourceObject> getResources();

  // Unknown fields
  @JsonAnyGetter
  public abstract Map<String, Object> getAllOtherFields();

  public abstract String getName();
}
