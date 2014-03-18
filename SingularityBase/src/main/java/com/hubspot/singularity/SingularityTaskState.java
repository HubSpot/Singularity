package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

public class SingularityTaskState extends SingularityTaskIdHolder {

  private final long timestamp;
  private final Optional<String> directory;
  private final Optional<SingularityTaskHealthcheckResult> lastHealthcheck;
  
  public static SingularityTaskState fromBytes(byte[] bytes, ObjectMapper objectMapper) throws Exception {
    return objectMapper.readValue(bytes, SingularityTaskState.class);
  }
  
  @JsonCreator
  public SingularityTaskState(@JsonProperty("taskId") SingularityTaskId taskId, @JsonProperty("timestamp") long timestamp, @JsonProperty("directory") Optional<String> directory, @JsonProperty("lastHealthcheck") Optional<SingularityTaskHealthcheckResult> lastHealthcheck) {
    super(taskId);
    this.timestamp = timestamp;
    this.directory = directory;
    this.lastHealthcheck = lastHealthcheck;
  }
  
  public Optional<SingularityTaskHealthcheckResult> getLastHealthcheck() {
    return lastHealthcheck;
  }

  public long getTimestamp() {
    return timestamp;
  }
  
  public Optional<String> getDirectory() {
    return directory;
  }

  @Override
  public String toString() {
    return "SingularityTaskState [timestamp=" + timestamp + ", taskId=" + getTaskId() + ", directory=" + directory + ", lastHealthcheck=" + lastHealthcheck + "]";
  }

}
