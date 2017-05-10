package com.hubspot.mesos.json;

import java.util.List;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hubspot.immutables.style.SingularityStyle;

@Immutable
@SingularityStyle
@JsonDeserialize(as = MesosExecutorObject.class)
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
