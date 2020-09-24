package com.hubspot.mesos.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class MesosAgentFrameworkObject extends MesosSlaveFrameworkObject {

  @JsonCreator
  public MesosAgentFrameworkObject(
    @JsonProperty("id") String id,
    @JsonProperty("executors") List<MesosExecutorObject> executors,
    @JsonProperty("completed_executors") List<MesosExecutorObject> completedExecutors
  ) {
    super(id, executors, completedExecutors);
  }
}
