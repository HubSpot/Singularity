package com.hubspot.singularity.data;

import com.google.common.base.Optional;

public class SingularityHistory {

  private final long timestamp;
  private final String statusUpdate;
  private final Optional<String> statusMessage;

  public SingularityHistory(long timestamp, String statusUpdate, Optional<String> statusMessage) {
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

}
