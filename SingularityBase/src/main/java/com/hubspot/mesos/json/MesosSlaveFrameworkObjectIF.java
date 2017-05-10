package com.hubspot.mesos.json;

import java.util.List;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hubspot.immutables.style.SingularityStyle;

@Immutable
@SingularityStyle
@JsonDeserialize(as = MesosSlaveFrameworkObject.class)
public interface MesosSlaveFrameworkObjectIF {

  String getId();

  List<MesosExecutorObject> getCompletedExecutors();

  @JsonProperty("completed_executors")
  List<MesosExecutorObject> getExecutors();
}
