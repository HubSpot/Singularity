package com.hubspot.singularity.expiring;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.api.SingularityScaleRequest;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Details about a scale action that will eventually revert")
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

  @Schema(description = "The instance count to update to when time has elapsed", nullable = true)
  public Optional<Integer> getRevertToInstances() {
    return revertToInstances;
  }

  @Schema(description = "If the scale action when updating instance count should also trigger a bounce", nullable = true, defaultValue = "false")
  public Optional<Boolean> getBounce() {
    return bounce;
  }

  @Override
  public String toString() {
    return "SingularityExpiringScale{" +
        "revertToInstances=" + revertToInstances +
        ", bounce=" + bounce +
        "} " + super.toString();
  }
}
