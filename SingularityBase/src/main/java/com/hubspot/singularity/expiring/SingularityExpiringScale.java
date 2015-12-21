package com.hubspot.singularity.expiring;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.api.SingularityScaleRequest;

public class SingularityExpiringScale extends SingularityExpiringParent {

  private final SingularityScaleRequest scaleRequest;
  private final Optional<Integer> revertToInstances;

  public SingularityExpiringScale(@JsonProperty("durationMillis") long durationMillis, @JsonProperty("requestId") String requestId, @JsonProperty("user") Optional<String> user,
      @JsonProperty("startMillis") long startMillis, @JsonProperty("scaleRequest") SingularityScaleRequest scaleRequest, @JsonProperty("revertToInstances") Optional<Integer> revertToInstances) {
    super(durationMillis, requestId, user, startMillis);

    this.revertToInstances = revertToInstances;
    this.scaleRequest = scaleRequest;
  }

  public SingularityScaleRequest getScaleRequest() {
    return scaleRequest;
  }

  public Optional<Integer> getRevertToInstances() {
    return revertToInstances;
  }

  @Override
  public String toString() {
    return "SingularityExpiringScale [scaleRequest=" + scaleRequest + ", revertToInstances=" + revertToInstances + "]";
  }

}
