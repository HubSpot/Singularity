package com.hubspot.singularity.expiring;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.api.SingularitySkipHealthchecksRequest;

public class SingularityExpiringSkipHealthchecks extends SingularityExpiringParent {

  private final SingularitySkipHealthchecksRequest skipHealthchecksRequest;
  private final Optional<Boolean> revertToSkipHealthchecks;

  public SingularityExpiringSkipHealthchecks(@JsonProperty("durationMillis") long durationMillis, @JsonProperty("requestId") String requestId, @JsonProperty("user") Optional<String> user,
      @JsonProperty("startMillis") long startMillis, @JsonProperty("skipHealthchecksRequest") SingularitySkipHealthchecksRequest skipHealthchecksRequest,
      @JsonProperty("revertToSkipHealthchecks") Optional<Boolean> revertToSkipHealthchecks) {
    super(durationMillis, requestId, user, startMillis);

    this.revertToSkipHealthchecks = revertToSkipHealthchecks;
    this.skipHealthchecksRequest = skipHealthchecksRequest;
  }

  public SingularitySkipHealthchecksRequest getSkipHealthchecksRequest() {
    return skipHealthchecksRequest;
  }

  public Optional<Boolean> getRevertToSkipHealthchecks() {
    return revertToSkipHealthchecks;
  }

  @Override
  public String toString() {
    return "SingularityExpiringSkipHealthchecks [skipHealthchecksRequest=" + skipHealthchecksRequest + ", revertToSkipHealthchecks=" + revertToSkipHealthchecks + "]";
  }

}
