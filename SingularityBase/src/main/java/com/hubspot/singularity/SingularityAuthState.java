package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityAuthState {
  private final Optional<Long> lastUpdatedAt;

  @JsonCreator
  public SingularityAuthState(@JsonProperty("lastUpdatedAt") Optional<Long> lastUpdatedAt) {
    this.lastUpdatedAt = lastUpdatedAt;
  }

  public Optional<Long> getLastUpdatedAt() {
    return lastUpdatedAt;
  }

  @Override
  public String toString() {
    return "SingularityAuthState[" +
            "lastUpdatedAt=" + lastUpdatedAt +
            ']';
  }
}
