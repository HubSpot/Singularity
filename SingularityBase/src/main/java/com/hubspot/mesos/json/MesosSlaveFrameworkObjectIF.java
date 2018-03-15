package com.hubspot.mesos.json;

import java.util.List;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.singularity.annotations.SingularityStyle;

@Immutable
@SingularityStyle
public interface MesosSlaveFrameworkObjectIF {

  String getId();

  List<MesosExecutorObject> getCompletedExecutors();

  @JsonProperty("completed_executors")
  List<MesosExecutorObject> getExecutors();
}
