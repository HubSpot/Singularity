package com.hubspot.singularity.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityScaleRequest {

  private final Optional<Integer> instances;
  private final Optional<Long> durationMillis;
  private final Optional<Boolean> skipHealthchecks;

  @JsonCreator
  public SingularityScaleRequest(@JsonProperty("instances") Optional<Integer> instances, @JsonProperty("durationMillis") Optional<Long> durationMillis,
      @JsonProperty("skipHealthchecks") Optional<Boolean> skipHealthchecks) {
    this.instances = instances;
    this.durationMillis = durationMillis;
    this.skipHealthchecks = skipHealthchecks;
  }

  public Optional<Boolean> getSkipHealthchecks() {
    return skipHealthchecks;
  }

  public Optional<Integer> getInstances() {
    return instances;
  }

  public Optional<Long> getDurationMillis() {
    return durationMillis;
  }

  @Override
  public String toString() {
    return "SingularityScaleRequest [instances=" + instances + ", durationMillis=" + durationMillis + ", skipHealthchecks=" + skipHealthchecks + "]";
  }

}
