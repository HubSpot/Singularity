package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityPendingDeploy {

  private final SingularityDeployMarker deployMarker;
  private final Optional<SingularityLoadBalancerUpdate> lastLoadBalancerUpdate;
  private final DeployState currentDeployState;

  @JsonCreator
  public SingularityPendingDeploy(@JsonProperty("deployMarker") SingularityDeployMarker deployMarker, @JsonProperty("lastLoadBalancerUpdate") Optional<SingularityLoadBalancerUpdate> lastLoadBalancerUpdate,
      @JsonProperty("currentDeployState") DeployState currentDeployState) {
    this.deployMarker = deployMarker;
    this.lastLoadBalancerUpdate = lastLoadBalancerUpdate;
    this.currentDeployState = currentDeployState;
  }

  public SingularityDeployMarker getDeployMarker() {
    return deployMarker;
  }

  public Optional<SingularityLoadBalancerUpdate> getLastLoadBalancerUpdate() {
    return lastLoadBalancerUpdate;
  }

  public DeployState getCurrentDeployState() {
    return currentDeployState;
  }

  @Override
  public String toString() {
    return "SingularityPendingDeploy [deployMarker=" + deployMarker + ", lastLoadBalancerUpdate=" + lastLoadBalancerUpdate + ", currentDeployState=" + currentDeployState + "]";
  }

}
