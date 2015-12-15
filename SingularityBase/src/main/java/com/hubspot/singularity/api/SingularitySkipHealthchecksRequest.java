package com.hubspot.singularity.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularitySkipHealthchecksRequest {

  private final Optional<Boolean> skipHealthchecks;
  private final Optional<Long> durationMillis;

  @JsonCreator
  public SingularitySkipHealthchecksRequest(@JsonProperty("skipHealthchecks") Optional<Boolean> skipHealthchecks, @JsonProperty("duration") Optional<Long> durationMillis) {
    this.skipHealthchecks = skipHealthchecks;
    this.durationMillis = durationMillis;
  }

  public Optional<Boolean> getSkipHealthchecks() {
    return skipHealthchecks;
  }

  public Optional<Long> getDurationMillis() {
    return durationMillis;
  }

  @Override
  public String toString() {
    return "SingularitySkipHealthchecksRequest [skipHealthchecks=" + skipHealthchecks + ", durationMillis=" + durationMillis + "]";
  }

}
