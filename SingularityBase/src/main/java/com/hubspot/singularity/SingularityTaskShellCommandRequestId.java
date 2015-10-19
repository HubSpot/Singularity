package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityTaskShellCommandRequestId extends SingularityId {

  private final SingularityTaskId taskId;
  private final String name;
  private final String safeName;
  private final long timestamp;

  @JsonCreator
  public SingularityTaskShellCommandRequestId(@JsonProperty("taskId") SingularityTaskId taskId, @JsonProperty("name") String name, @JsonProperty("timestamp") long timestamp) {
    this.taskId = taskId;
    this.timestamp = timestamp;
    this.name = name;
    this.safeName = name.replace("/", "");
  }

  @Override
  @JsonIgnore
  public String getId() {
    return String.format("%s-%s", getTaskId(), getSubIdForTaskHistory());
  }

  @JsonIgnore
  public String getSubIdForTaskHistory() {
    return String.format("%s-%s", safeName, getTimestamp());
  }

  public SingularityTaskId getTaskId() {
    return taskId;
  }

  public String getName() {
    return name;
  }

  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public String toString() {
    return "SingularityTaskShellCommandRequestId [taskId=" + taskId + ", name=" + name + ", timestamp=" + timestamp + "]";
  }

}
