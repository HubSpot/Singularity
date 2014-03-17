package com.hubspot.singularity;

import java.util.List;
import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class SingularityTaskHistory {

  private final List<SingularityTaskHistoryUpdate> taskUpdates;
  private final long timestamp;
  private final Optional<String> directory;
  private final SingularityTask task;

  @JsonCreator
  public SingularityTaskHistory(@JsonProperty("taskUpdates") List<SingularityTaskHistoryUpdate> taskUpdates, @JsonProperty("timestamp") long timestamp, @JsonProperty("task") SingularityTask task,
      @JsonProperty("directory") Optional<String> directory) {
    this.taskUpdates = taskUpdates;
    this.timestamp = timestamp;
    this.task = task;
    this.directory = directory;
  }

  public List<SingularityTaskHistoryUpdate> getTaskUpdates() {
    return taskUpdates;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public SingularityTask getTask() {
    return task;
  }

  public Optional<String> getDirectory() {
    return directory;
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
    return "SingularityTaskHistory [taskUpdates=" + taskUpdates + ", timestamp=" + timestamp + ", directory=" + directory + ", task=" + task + "]";
  }

}
