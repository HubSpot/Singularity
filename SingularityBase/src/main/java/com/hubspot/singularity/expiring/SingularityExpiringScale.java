package com.hubspot.singularity.expiring;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.api.SingularityScaleRequest;

public class SingularityExpiringScale extends SingularityExpiringParent<SingularityScaleRequest> {

  private final Optional<Integer> revertToInstances;

  public SingularityExpiringScale(@JsonProperty("requestId") String requestId, @JsonProperty("user") Optional<String> user,
      @JsonProperty("startMillis") long startMillis, @JsonProperty("expiringAPIRequestObject") SingularityScaleRequest scaleRequest, @JsonProperty("revertToInstances") Optional<Integer> revertToInstances,
      @JsonProperty("actionId") String actionId) {
    super(scaleRequest, requestId, user, startMillis, actionId);

    this.revertToInstances = revertToInstances;
  }

  public Optional<Integer> getRevertToInstances() {
    return revertToInstances;
  }

  @Override
  public String toString() {
    return "SingularityExpiringScale [revertToInstances=" + revertToInstances + "]";
  }

}
