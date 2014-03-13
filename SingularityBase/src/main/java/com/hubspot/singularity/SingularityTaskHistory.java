package com.hubspot.singularity;

import java.util.List;
import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class SingularityTaskHistory extends SingularityJsonObject {

  private final List<SingularityTaskHistoryUpdate> taskUpdates;
  private final SingularityTaskState taskState;
  private final SingularityTask task;

  @JsonCreator
  public SingularityTaskHistory(@JsonProperty("taskUpdates") List<SingularityTaskHistoryUpdate> taskUpdates, @JsonProperty("taskState") SingularityTaskState taskState, @JsonProperty("task") SingularityTask task) {
    this.taskUpdates = taskUpdates;
    this.taskState = taskState;
    this.task = task;
  }

  public List<SingularityTaskHistoryUpdate> getTaskUpdates() {
    return taskUpdates;
  }

  public SingularityTaskState getTaskState() {
    return taskState;
  }

  public SingularityTask getTask() {
    return task;
  }


  public List<Map<String, String>> getTaskHistoryJade() {
    List<Map<String, String>> output = Lists.newArrayList();

    for (SingularityTaskHistoryUpdate taskUpdate : taskUpdates) {
      Map<String, String> formatted = Maps.newHashMap();
      Date date = new Date(taskUpdate.getTimestamp());
      formatted.put("date", date.toString());
      formatted.put("update", taskUpdate.getStatusUpdate());
      output.add(formatted);
    }
    return output;
  }

  @Override
  public String toString() {
    return "SingularityTaskHistory [taskUpdates=" + taskUpdates + ", taskState=" + taskState + ", task=" + task + "]";
  }
  
}
