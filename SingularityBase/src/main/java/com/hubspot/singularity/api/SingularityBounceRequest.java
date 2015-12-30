package com.hubspot.singularity.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityBounceRequest extends SingularityExpiringRequestParent {

  private final Optional<Boolean> incremental;
  private final Optional<Boolean> skipHealthchecks;

  @JsonCreator
  public SingularityBounceRequest(@JsonProperty("incremental") Optional<Boolean> incremental, @JsonProperty("skipHealthchecks") Optional<Boolean> skipHealthchecks,
      @JsonProperty("durationMillis") Optional<Long> durationMillis, @JsonProperty("actionId") Optional<String> actionId, @JsonProperty("message") Optional<String> message) {
    super(durationMillis, actionId, message);
    this.incremental = incremental;
    this.skipHealthchecks = skipHealthchecks;
  }

  public Optional<Boolean> getIncremental() {
    return incremental;
  }

  public Optional<Boolean> getSkipHealthchecks() {
    return skipHealthchecks;
  }

  @Override
  public String toString() {
    return "SingularityBounceRequest [incremental=" + incremental + ", skipHealthchecks=" + skipHealthchecks + ", toString()=" + super.toString() + "]";
  }

}
