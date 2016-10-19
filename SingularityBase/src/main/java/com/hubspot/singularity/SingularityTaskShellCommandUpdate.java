package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityTaskShellCommandUpdate {

  public enum UpdateType {
    INVALID(true), ACKED(false), STARTED(false), FINISHED(true), FAILED(true);

    private final boolean finished;

    UpdateType(boolean finished) {
      this.finished = finished;
    }

    public boolean isFinished() {
      return finished;
    }
  }

  private final SingularityTaskShellCommandRequestId shellRequestId;
  private final long timestamp;
  private final Optional<String> message;
  private final Optional<String> outputFilename;
  private final UpdateType updateType;

  @JsonCreator
  public SingularityTaskShellCommandUpdate(@JsonProperty("shellRequestId") SingularityTaskShellCommandRequestId shellRequestId, @JsonProperty("timestamp") long timestamp,
      @JsonProperty("message") Optional<String> message, @JsonProperty("outputFilename") Optional<String> outputFilename, @JsonProperty("updateType") UpdateType updateType) {
    this.shellRequestId = shellRequestId;
    this.timestamp = timestamp;
    this.message = message;
    this.outputFilename = outputFilename;
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

  public Optional<String> getOutputFilename() {
    return outputFilename;
  }

  public UpdateType getUpdateType() {
    return updateType;
  }

  @Override
  public String toString() {
    return "SingularityTaskShellCommandUpdate[" +
            "shellRequestId=" + shellRequestId +
            ", timestamp=" + timestamp +
            ", message=" + message +
            ", outputFilename=" + outputFilename +
            ", updateType=" + updateType +
            ']';
  }
}
