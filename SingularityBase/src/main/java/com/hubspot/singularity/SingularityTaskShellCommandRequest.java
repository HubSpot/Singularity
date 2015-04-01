package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityTaskShellCommandRequest extends SingularityId {

  private final SingularityTaskId taskId;
  private final Optional<String> user;
  private final SingularityShellCommand shellCommand;
  private final long timestamp;

  @JsonCreator
  public SingularityTaskShellCommandRequest(@JsonProperty("taskId") SingularityTaskId taskId, @JsonProperty("user") Optional<String> user, @JsonProperty("timestamp") long timestamp,
      @JsonProperty("shellCommand") SingularityShellCommand shellCommand) {
    this.taskId = taskId;
    this.user = user;
    this.timestamp = timestamp;
    this.shellCommand = shellCommand;
  }

  @Override
  public String getId() {
    return getShellCommand().getName().replace("/", "") + getTimestamp(); // make sure the name is safe for ZK.
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
