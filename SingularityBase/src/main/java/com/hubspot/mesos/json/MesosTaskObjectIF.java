package com.hubspot.mesos.json;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.mesos.MesosResourcesObject;
import com.hubspot.singularity.annotations.SingularityStyle;

@Immutable
@SingularityStyle
public interface MesosTaskObjectIF {

  MesosResourcesObject getResources();

  String getState();

  String getId();

  String getName();

  @JsonProperty("slave_id")
  String getSlaveId();

  @JsonProperty("framework_id")
  String getFrameworkId();

  @JsonProperty("executor_id")
  String getExecutorId();
}
