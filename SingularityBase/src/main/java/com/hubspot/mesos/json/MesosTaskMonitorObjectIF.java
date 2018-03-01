package com.hubspot.mesos.json;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.singularity.annotations.SingularityStyle;

@Immutable
@SingularityStyle
public interface MesosTaskMonitorObjectIF {

  @JsonProperty("executor_id")
  String getExecutorId();

  @JsonProperty("executor_name")

  String getExecutorName();

  @JsonProperty("framework_id")

  String getFrameworkId();

  String getSource();

  MesosTaskStatisticsObject getStatistics();
}
