package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityMachineStateHistoryUpdate {

  private final String objectId;
  private final MachineState state;
  private final Optional<String> user;
  private final Optional<String> message;
  private final long timestamp;

  @JsonCreator
  public SingularityMachineStateHistoryUpdate(@JsonProperty("objectId") String objectId, @JsonProperty("state") MachineState state, @JsonProperty("timestamp") long timestamp,
      @JsonProperty("user") Optional<String> user, @JsonProperty("message") Optional<String> message) {
    this.objectId = objectId;
    this.state = state;
    this.timestamp = timestamp;
    this.user = user;
    this.message = message;
  }

  public Optional<String> getMessage() {
    return message;
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
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((message == null) ? 0 : message.hashCode());
    result = prime * result + ((objectId == null) ? 0 : objectId.hashCode());
    result = prime * result + ((state == null) ? 0 : state.hashCode());
    result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
    result = prime * result + ((user == null) ? 0 : user.hashCode());
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
    SingularityMachineStateHistoryUpdate other = (SingularityMachineStateHistoryUpdate) obj;
    if (message == null) {
      if (other.message != null) {
        return false;
      }
    } else if (!message.equals(other.message)) {
      return false;
    }
    if (objectId == null) {
      if (other.objectId != null) {
        return false;
      }
    } else if (!objectId.equals(other.objectId)) {
      return false;
    }
    if (state != other.state) {
      return false;
    }
    if (timestamp != other.timestamp) {
      return false;
    }
    if (user == null) {
      if (other.user != null) {
        return false;
      }
    } else if (!user.equals(other.user)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "SingularityMachineStateHistoryUpdate [objectId=" + objectId + ", state=" + state + ", user=" + user + ", message=" + message + ", timestamp=" + timestamp + "]";
  }

}
