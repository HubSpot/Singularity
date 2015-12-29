package com.hubspot.singularity.expiring;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.api.SingularitySkipHealthchecksRequest;

public class SingularityExpiringSkipHealthchecks extends SingularityExpiringParent<SingularitySkipHealthchecksRequest> {

  private final Optional<Boolean> revertToSkipHealthchecks;

  public SingularityExpiringSkipHealthchecks(@JsonProperty("requestId") String requestId, @JsonProperty("user") Optional<String> user,
      @JsonProperty("startMillis") long startMillis, @JsonProperty("expiringAPIRequestObject") SingularitySkipHealthchecksRequest skipHealthchecksRequest,
      @JsonProperty("revertToSkipHealthchecks") Optional<Boolean> revertToSkipHealthchecks, @JsonProperty("actionId") String actionId) {
    super(skipHealthchecksRequest, requestId, user, startMillis, actionId);

    this.revertToSkipHealthchecks = revertToSkipHealthchecks;
  }

  public Optional<Boolean> getRevertToSkipHealthchecks() {
    return revertToSkipHealthchecks;
  }

  @Override
  public String toString() {
    return "SingularityExpiringSkipHealthchecks [revertToSkipHealthchecks=" + revertToSkipHealthchecks + ", toString()=" + super.toString() + "]";
  }

}
