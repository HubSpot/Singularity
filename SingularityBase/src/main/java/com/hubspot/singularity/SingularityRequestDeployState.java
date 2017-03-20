package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

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

  public String getRequestId() {
    return requestId;
  }

  public Optional<SingularityDeployMarker> getActiveDeploy() {
    return activeDeploy;
  }

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
