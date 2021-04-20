package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import java.util.Objects;

public class DeployAcceptanceResult {
  private final DeployAcceptanceState state;
  private final String message;

  @JsonCreator
  public DeployAcceptanceResult(
    @JsonProperty("state") DeployAcceptanceState state,
    @JsonProperty("message") String message
  ) {
    this.state = state;
    this.message = message;
  }

  public DeployAcceptanceState getState() {
    return state;
  }

  public String getMessage() {
    return message;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DeployAcceptanceResult that = (DeployAcceptanceResult) o;
    return state == that.state && Objects.equals(message, that.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(state, message);
  }

  @Override
  public String toString() {
    return MoreObjects
      .toStringHelper(this)
      .add("state", state)
      .add("message", message)
      .toString();
  }
}
