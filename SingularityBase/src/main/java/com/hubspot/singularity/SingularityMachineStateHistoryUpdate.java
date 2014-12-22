package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityMachineStateHistoryUpdate {

  private final String objectId;
  private final MachineState state;
  private final Optional<String> user;
  private final long timestamp;

  @JsonCreator
  public SingularityMachineStateHistoryUpdate(@JsonProperty("objectId") String objectId, @JsonProperty("state") MachineState state, @JsonProperty("timestamp") long timestamp,
      @JsonProperty("user") Optional<String> user) {
    this.objectId = objectId;
    this.state = state;
    this.timestamp = timestamp;
    this.user = user;
  }

  public Optional<String> getUser() {
    return user;
  }

  public String getObjectId() {
    return objectId;
  }

  public MachineState getState() {
    return state;
  }

  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public String toString() {
    return "SingularityMachineStateHistoryUpdate [objectId=" + objectId + ", state=" + state + ", timestamp=" + timestamp + "]";
  }

}
