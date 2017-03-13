package com.hubspot.singularity.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class SingularitySkipHealthchecksRequest extends SingularityExpiringRequestParent {

  private final Optional<Boolean> skipHealthchecks;

  @JsonCreator
  public SingularitySkipHealthchecksRequest(@JsonProperty("skipHealthchecks") Optional<Boolean> skipHealthchecks,
      @JsonProperty("durationMillis") Optional<Long> durationMillis, @JsonProperty("actionId") Optional<String> actionId, @JsonProperty("message") Optional<String> message) {
    super(durationMillis, actionId, message);
    this.skipHealthchecks = skipHealthchecks;
  }

  @ApiModelProperty(required=false, value="If set to true, healthchecks will be skipped for all tasks for this request until reversed")
  public Optional<Boolean> getSkipHealthchecks() {
    return skipHealthchecks;
  }

  @Override
  public String toString() {
    return "SingularitySkipHealthchecksRequest{" +
        "skipHealthchecks=" + skipHealthchecks +
        "} " + super.toString();
  }
}
