package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityTaskShellCommandUpdate {

  public enum UpdateType {
    INVALID, ACKED, STARTED, FINISHED, FAILED;
  }

  private final SingularityTaskShellCommandRequestId shellRequestId;
  private final long timestamp;
  private final Optional<String> message;
  private final UpdateType updateType;

  @JsonCreator
  public SingularityTaskShellCommandUpdate(@JsonProperty("shellRequestId") SingularityTaskShellCommandRequestId shellRequestId, @JsonProperty("timestamp") long timestamp,
      @JsonProperty("message") Optional<String> message, @JsonProperty("updateType") UpdateType updateType) {
    this.shellRequestId = shellRequestId;
    this.timestamp = timestamp;
    this.message = message;
    this.updateType = updateType;
  }

  public SingularityTaskShellCommandRequestId getShellRequestId() {
    return shellRequestId;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public Optional<String> getMessage() {
    return message;
  }

  public UpdateType getUpdateType() {
    return updateType;
  }

  @Override
  public String toString() {
    return "SingularityTaskShellCommandUpdate [shellRequestId=" + shellRequestId + ", timestamp=" + timestamp + ", message=" + message + ", updateType=" + updateType + "]";
  }

}
