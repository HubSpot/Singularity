package com.hubspot.mesos.json;

import java.util.List;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.mesos.MesosResourcesObject;
import com.hubspot.singularity.annotations.SingularityStyle;

@Immutable
@SingularityStyle
public interface MesosExecutorObjectIF {
  String getId();

  String getName();

  String getContainer();

  String getDirectory();

  MesosResourcesObject getResources();

  List<MesosTaskObject> getTasks();

  @JsonProperty("completed_tasks")
  List<MesosTaskObject> getCompletedTasks();
}
