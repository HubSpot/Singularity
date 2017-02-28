package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityTaskShellCommandRequestId {

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
  public int hashCode() {
    return getId().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    SingularityTaskShellCommandRequestId other = (SingularityTaskShellCommandRequestId) obj;
    return getId().equals(other.getId());
  }

  @Override
  public String toString() {
    return "SingularityTaskShellCommandRequestId{" +
        "taskId=" + taskId +
        ", name='" + name + '\'' +
        ", safeName='" + safeName + '\'' +
        ", timestamp=" + timestamp +
        "} " + super.toString();
  }
}
