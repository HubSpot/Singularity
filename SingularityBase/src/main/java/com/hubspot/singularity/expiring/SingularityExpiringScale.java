package com.hubspot.singularity.expiring;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.api.SingularityScaleRequest;

public class SingularityExpiringScale extends SingularityExpiringRequestActionParent<SingularityScaleRequest> {

  private final Optional<Integer> revertToInstances;
  private final Optional<Boolean> bounce;

  public SingularityExpiringScale(@JsonProperty("requestId") String requestId, @JsonProperty("user") Optional<String> user,
      @JsonProperty("startMillis") long startMillis, @JsonProperty("expiringAPIRequestObject") SingularityScaleRequest scaleRequest, @JsonProperty("revertToInstances") Optional<Integer> revertToInstances,
      @JsonProperty("actionId") String actionId, @JsonProperty("bounce") Optional<Boolean> bounce) {
    super(scaleRequest, user, startMillis, actionId, requestId);

    this.revertToInstances = revertToInstances;
    this.bounce = bounce;
  }

  public Optional<Integer> getRevertToInstances() {
    return revertToInstances;
  }

  public Optional<Boolean> getBounce() {
    return bounce;
  }

  @Override
  public String toString() {
    return "SingularityExpiringScale [revertToInstances=" + revertToInstances + ", bounce=" + bounce + "]";
  }

}
