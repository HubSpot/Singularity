package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

public class SingularityTaskHistoryUpdate extends SingularityJsonObject {

  private final String taskId;
  private final long timestamp;
  private final String statusUpdate;
  private final Optional<String> statusMessage;

  public static SingularityTaskHistoryUpdate fromBytes(byte[] bytes, ObjectMapper objectMapper) throws Exception {
    return objectMapper.readValue(bytes, SingularityTaskHistoryUpdate.class);
  }
  
  @JsonCreator
  public SingularityTaskHistoryUpdate(@JsonProperty("taskId") String taskId, @JsonProperty("timestamp") long timestamp, @JsonProperty("statusUpdate") String statusUpdate, @JsonProperty("statusMessage") Optional<String> statusMessage) {
    this.taskId = taskId;
    this.timestamp = timestamp;
    this.statusUpdate = statusUpdate;
    this.statusMessage = statusMessage;
  }
  
  public String getTaskId() {
    return taskId;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public String getStatusUpdate() {
    return statusUpdate;
  }

  public Optional<String> getStatusMessage() {
    return statusMessage;
  }

  @Override
  public String toString() {
    return "SingularityTaskHistoryUpdate [taskId=" + taskId + ", timestamp=" + timestamp + ", statusUpdate=" + statusUpdate + ", statusMessage=" + statusMessage + "]";
  }

}
