package com.hubspot.singularity.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityScaleRequest extends SingularityExpiringRequestParent {

  private final Optional<Integer> instances;
  private final Optional<Boolean> skipHealthchecks;

  @JsonCreator
  public SingularityScaleRequest(@JsonProperty("instances") Optional<Integer> instances, @JsonProperty("durationMillis") Optional<Long> durationMillis,
      @JsonProperty("skipHealthchecks") Optional<Boolean> skipHealthchecks, @JsonProperty("actionId") Optional<String> actionId, @JsonProperty("message") Optional<String> message) {
    super(durationMillis, actionId, message);
    this.instances = instances;
    this.skipHealthchecks = skipHealthchecks;
  }

  public Optional<Boolean> getSkipHealthchecks() {
    return skipHealthchecks;
  }

  public Optional<Integer> getInstances() {
    return instances;
  }

  @Override
  public String toString() {
    return "SingularityScaleRequest [instances=" + instances + ", skipHealthchecks=" + skipHealthchecks + ", toString()=" + super.toString() + "]";
  }

}
