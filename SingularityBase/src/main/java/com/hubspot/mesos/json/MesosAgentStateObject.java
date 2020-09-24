package com.hubspot.mesos.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class MesosAgentStateObject extends MesosSlaveStateObject {

  @JsonCreator
  public MesosAgentStateObject(
    @JsonProperty("id") String id,
    @JsonProperty("pid") String pid,
    @JsonProperty("hostname") String hostname,
    @JsonProperty("start_time") long startTime,
    @JsonProperty("resources") MesosResourcesObject resources,
    @JsonProperty("frameworks") List<MesosAgentFrameworkObject> frameworks,
    @JsonProperty("finished_tasks") int finishedTasks,
    @JsonProperty("lost_tasks") int lostTasks,
    @JsonProperty("started_tasks") int startedTasks,
    @JsonProperty("failed_tasks") int failedTasks,
    @JsonProperty("killed_tasks") int killedTasks,
    @JsonProperty("staged_tasks") int stagedTasks
  ) {
    super(
      id,
      pid,
      hostname,
      startTime,
      resources,
      frameworks,
      finishedTasks,
      lostTasks,
      startedTasks,
      failedTasks,
      killedTasks,
      stagedTasks
    );
  }
}
