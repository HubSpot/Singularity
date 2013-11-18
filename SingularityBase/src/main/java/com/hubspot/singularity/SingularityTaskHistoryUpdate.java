package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityTaskHistoryUpdate {

  private final long timestamp;
  private final String statusUpdate;
  private final Optional<String> statusMessage;

  @JsonCreator
  public SingularityTaskHistoryUpdate(@JsonProperty("timestamp") long timestamp, @JsonProperty("statusUpdate") String statusUpdate, @JsonProperty("statusMessage") Optional<String> statusMessage) {
    this.timestamp = timestamp;
    this.statusUpdate = statusUpdate;
    this.statusMessage = statusMessage;
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
    return "SingularityTaskUpdate [timestamp=" + timestamp + ", statusUpdate=" + statusUpdate + ", statusMessage=" + statusMessage + "]";
  }

}
