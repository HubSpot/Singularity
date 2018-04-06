package com.hubspot.singularity.expiring;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.api.SingularitySkipHealthchecksRequest;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Details about a skip healthchecks action that will eventually revert")
public class SingularityExpiringSkipHealthchecks extends SingularityExpiringRequestActionParent<SingularitySkipHealthchecksRequest> {

  private final Optional<Boolean> revertToSkipHealthchecks;

  public SingularityExpiringSkipHealthchecks(@JsonProperty("requestId") String requestId, @JsonProperty("user") Optional<String> user,
                                             @JsonProperty("startMillis") long startMillis, @JsonProperty("expiringAPIRequestObject") SingularitySkipHealthchecksRequest skipHealthchecksRequest,
                                             @JsonProperty("revertToSkipHealthchecks") Optional<Boolean> revertToSkipHealthchecks, @JsonProperty("actionId") String actionId) {
    super(skipHealthchecksRequest, user, startMillis, actionId, requestId);

    this.revertToSkipHealthchecks = revertToSkipHealthchecks;
  }

  @Schema(description = "If the revert should update skipHealthchecks to a `true` or `false` state", nullable = true)
  public Optional<Boolean> getRevertToSkipHealthchecks() {
    return revertToSkipHealthchecks;
  }

  @Override
  public String toString() {
    return "SingularityExpiringSkipHealthchecks{" +
        "revertToSkipHealthchecks=" + revertToSkipHealthchecks +
        "} " + super.toString();
  }
}
