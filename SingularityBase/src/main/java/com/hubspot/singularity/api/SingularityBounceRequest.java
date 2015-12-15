package com.hubspot.singularity.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityBounceRequest {

  private final Optional<Boolean> incremental;
  private final Optional<Boolean> skipHealthchecks;
  private final Optional<Long> durationMillis;

  @JsonCreator
  public SingularityBounceRequest(@JsonProperty("incremental") Optional<Boolean> incremental,
      @JsonProperty("skipHealthchecks") Optional<Boolean> skipHealthchecks, @JsonProperty("requestId") String requestId, @JsonProperty("durationMillis") Optional<Long> durationMillis) {
    this.incremental = incremental;
    this.skipHealthchecks = skipHealthchecks;
    this.durationMillis = durationMillis;
  }

  public Optional<Long> getDurationMillis() {
    return durationMillis;
  }

  public Optional<Boolean> getIncremental() {
    return incremental;
  }

  public Optional<Boolean> getSkipHealthchecks() {
    return skipHealthchecks;
  }

  @Override
  public String toString() {
    return "SingularityBounceRequest [incremental=" + incremental + ", skipHealthchecks=" + skipHealthchecks + ", durationMillis=" + durationMillis + "]";
  }

}
