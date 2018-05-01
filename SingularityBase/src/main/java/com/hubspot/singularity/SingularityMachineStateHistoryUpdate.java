package com.hubspot.singularity;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Describes an update to the state of a rack or slave")
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

  @Schema(description = "An optional message describing this update", nullable = true)
  public Optional<String> getMessage() {
    return message;
  }

  @Schema(description = "The user who triggered this update", nullable = true)
  public Optional<String> getUser() {
    return user;
  }

  @Schema(description = "The id of the machine")
  public String getObjectId() {
    return objectId;
  }

  @Schema(description = "The state of the machine")
  public MachineState getState() {
    return state;
  }

  @Schema(description = "the timestamp of this state update")
  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityMachineStateHistoryUpdate that = (SingularityMachineStateHistoryUpdate) o;
    return timestamp == that.timestamp &&
        Objects.equals(objectId, that.objectId) &&
        state == that.state &&
        Objects.equals(user, that.user) &&
        Objects.equals(message, that.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(objectId, state, user, message, timestamp);
  }

  @Override
  public String toString() {
    return "SingularityMachineStateHistoryUpdate{" +
        "objectId='" + objectId + '\'' +
        ", state=" + state +
        ", user=" + user +
        ", message=" + message +
        ", timestamp=" + timestamp +
        '}';
  }
}
