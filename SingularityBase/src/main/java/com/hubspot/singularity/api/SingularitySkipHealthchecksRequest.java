package com.hubspot.singularity.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularitySkipHealthchecksRequest extends SingularityExpiringRequestParent {

  private final Optional<Boolean> skipHealthchecks;

  @JsonCreator
  public SingularitySkipHealthchecksRequest(@JsonProperty("skipHealthchecks") Optional<Boolean> skipHealthchecks,
      @JsonProperty("durationMillis") Optional<Long> durationMillis, @JsonProperty("actionId") Optional<String> actionId, @JsonProperty("message") Optional<String> message) {
    super(durationMillis, actionId, message);
    this.skipHealthchecks = skipHealthchecks;
  }

  public Optional<Boolean> getSkipHealthchecks() {
    return skipHealthchecks;
  }

  @Override
  public String toString() {
    return "SingularitySkipHealthchecksRequest [skipHealthchecks=" + skipHealthchecks + ", toString()=" + super.toString() + "]";
  }

}
