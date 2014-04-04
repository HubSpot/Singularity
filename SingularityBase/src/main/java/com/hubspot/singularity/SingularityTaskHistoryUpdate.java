package com.hubspot.singularity;

import java.io.IOException;

import org.apache.mesos.Protos.TaskState;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ComparisonChain;
import com.hubspot.mesos.MesosUtils;

public class SingularityTaskHistoryUpdate extends SingularityTaskIdHolder implements Comparable<SingularityTaskHistoryUpdate> {

  private final long timestamp;
  private final TaskState taskState;
  private final Optional<String> statusMessage;

  public static SingularityTaskHistoryUpdate fromBytes(byte[] bytes, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(bytes, SingularityTaskHistoryUpdate.class);
    } catch (IOException e) {
      throw new SingularityJsonException(e);
    }
  }

  public enum SimplifiedTaskState {
    UNKNOWN, WAITING, RUNNING, DONE
  }
  
  public static SimplifiedTaskState getCurrentState(Iterable<SingularityTaskHistoryUpdate> updates) {
    SimplifiedTaskState state = SimplifiedTaskState.UNKNOWN;
    
    if (updates == null) {
      return state;
    }
    
    for (SingularityTaskHistoryUpdate update : updates) {
      if (MesosUtils.isTaskDone(update.getTaskState())) {
        return SimplifiedTaskState.DONE;
      } else if (update.getTaskState() == TaskState.TASK_RUNNING) {
        state = SimplifiedTaskState.RUNNING;
      } else if (state == SimplifiedTaskState.UNKNOWN) {
        state = SimplifiedTaskState.WAITING;
      }
    }
    
    return state;
  }
  
  @JsonCreator
  public SingularityTaskHistoryUpdate(@JsonProperty("taskId") SingularityTaskId taskId, @JsonProperty("timestamp") long timestamp, @JsonProperty("taskState") TaskState taskState, @JsonProperty("statusMessage") Optional<String> statusMessage) {
    super(taskId);
    this.timestamp = timestamp;
    this.taskState = taskState;
    this.statusMessage = statusMessage;
  }

  @Override
  public int compareTo(SingularityTaskHistoryUpdate o) {
    return ComparisonChain.start()
        .compare(timestamp, o.getTimestamp())
        .compare(getTaskId().getId(), o.getTaskId().getId())
        .result();
  }

  public long getTimestamp() {
    return timestamp;
  }

  public TaskState getTaskState() {
    return taskState;
  }

  public Optional<String> getStatusMessage() {
    return statusMessage;
  }

  @Override
  public String toString() {
    return "SingularityTaskHistoryUpdate [timestamp=" + timestamp + ", taskState=" + taskState + ", statusMessage=" + statusMessage + "]";
  }
  
}
