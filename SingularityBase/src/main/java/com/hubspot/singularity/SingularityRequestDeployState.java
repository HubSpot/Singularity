package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Data about active and pending deploys")
public class SingularityRequestDeployState {

  private final String requestId;

  private final Optional<SingularityDeployMarker> activeDeploy;
  private final Optional<SingularityDeployMarker> pendingDeploy;

  @JsonCreator
  public SingularityRequestDeployState(@JsonProperty("requestId") String requestId, @JsonProperty("activeDeploy") Optional<SingularityDeployMarker> activeDeploy, @JsonProperty("pendingDeploy") Optional<SingularityDeployMarker> pendingDeploy) {
    this.requestId = requestId;
    this.activeDeploy = activeDeploy;
    this.pendingDeploy = pendingDeploy;
  }

  @Schema(description = "The request these deploys belong to")
  public String getRequestId() {
    return requestId;
  }

  @Schema(description = "Uniquely identifies the active deploy if one is present")
  public Optional<SingularityDeployMarker> getActiveDeploy() {
    return activeDeploy;
  }

  @Schema(description = "Uniquely identifies the pending deploy if one is present")
  public Optional<SingularityDeployMarker> getPendingDeploy() {
    return pendingDeploy;
  }

  @Override
  public String toString() {
    return "SingularityRequestDeployState{" +
        "requestId='" + requestId + '\'' +
        ", activeDeploy=" + activeDeploy +
        ", pendingDeploy=" + pendingDeploy +
        '}';
  }
}
