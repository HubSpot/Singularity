package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.primitives.Longs;

public class SingularityTaskShellCommandRequest extends SingularityFrameworkMessage implements Comparable<SingularityTaskShellCommandRequest> {

  private final SingularityTaskId taskId;
  private final Optional<String> user;
  private final SingularityShellCommand shellCommand;
  private final long timestamp;
  private final SingularityTaskShellCommandRequestId id;

  @JsonCreator
  public SingularityTaskShellCommandRequest(@JsonProperty("taskId") SingularityTaskId taskId, @JsonProperty("user") Optional<String> user, @JsonProperty("timestamp") long timestamp,
      @JsonProperty("shellCommand") SingularityShellCommand shellCommand) {
    this.taskId = taskId;
    this.user = user;
    this.timestamp = timestamp;
    this.shellCommand = shellCommand;
    this.id = new SingularityTaskShellCommandRequestId(taskId, shellCommand.getName(), timestamp);
  }

  @JsonIgnore
  public SingularityTaskShellCommandRequestId getId() {
    return id;
  }

  @Override
  public int compareTo(SingularityTaskShellCommandRequest o) {
    return Longs.compare(o.getTimestamp(), timestamp);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    return result;
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
    SingularityTaskShellCommandRequest other = (SingularityTaskShellCommandRequest) obj;
    if (id == null) {
      if (other.id != null) {
        return false;
      }
    } else if (!id.equals(other.id)) {
      return false;
    }
    return true;
  }

  public SingularityTaskId getTaskId() {
    return taskId;
  }

  public Optional<String> getUser() {
    return user;
  }

  public SingularityShellCommand getShellCommand() {
    return shellCommand;
  }

  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public String toString() {
    return "SingularityTaskShellCommandRequest [taskId=" + taskId + ", user=" + user + ", shellCommand=" + shellCommand + ", timestamp=" + timestamp + "]";
  }

}
