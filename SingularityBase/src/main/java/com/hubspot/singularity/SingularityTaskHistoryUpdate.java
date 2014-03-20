package com.hubspot.singularity;

import org.apache.mesos.Protos.TaskState;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.hubspot.mesos.MesosUtils;

public class SingularityTaskHistoryUpdate extends SingularityTaskIdHolder {

  private final long timestamp;
  private final TaskState statusUpdate;
  private final Optional<String> statusMessage;

  public static SingularityTaskHistoryUpdate fromBytes(byte[] bytes, ObjectMapper objectMapper) throws Exception {
    return objectMapper.readValue(bytes, SingularityTaskHistoryUpdate.class);
  }

  public enum SimplifiedTaskState {
    WAITING, RUNNING, DONE
  }
  
  public static SimplifiedTaskState getCurrentState(Iterable<SingularityTaskHistoryUpdate> updates) {
    SimplifiedTaskState state = SimplifiedTaskState.WAITING;
    
    for (SingularityTaskHistoryUpdate update : updates) {
      if (MesosUtils.isTaskDone(update.getTaskStateEnum())) {
        return SimplifiedTaskState.DONE;
      } else if (update.getTaskStateEnum() == TaskState.TASK_RUNNING) {
        state = SimplifiedTaskState.RUNNING;
      }
    }
    
    return state;
  }
  
  @JsonCreator
  public SingularityTaskHistoryUpdate(@JsonProperty("taskId") SingularityTaskId taskId, @JsonProperty("timestamp") long timestamp, @JsonProperty("statusUpdate") String statusUpdate, @JsonProperty("statusMessage") Optional<String> statusMessage) {
    super(taskId);
    this.timestamp = timestamp;
    this.statusUpdate = TaskState.valueOf(statusUpdate);
    this.statusMessage = statusMessage;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public String getStatusUpdate() {
    return statusUpdate.name();
  }

  @JsonIgnore
  public TaskState getTaskStateEnum() {
    return statusUpdate;
  }

  public Optional<String> getStatusMessage() {
    return statusMessage;
  }

  @Override
  public String toString() {
    return "SingularityTaskHistoryUpdate [taskId=" + getTaskId() + ", timestamp=" + timestamp + ", statusUpdate=" + statusUpdate + ", statusMessage=" + statusMessage + "]";
  }

}
