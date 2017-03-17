package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityPendingDeploy {

  private final SingularityDeployMarker deployMarker;
  private final Optional<SingularityLoadBalancerUpdate> lastLoadBalancerUpdate;
  private final DeployState currentDeployState;
  private final Optional<SingularityDeployProgress> deployProgress;
  private final Optional<SingularityRequest> updatedRequest;

  @JsonCreator
  public SingularityPendingDeploy(@JsonProperty("deployMarker") SingularityDeployMarker deployMarker, @JsonProperty("lastLoadBalancerUpdate") Optional<SingularityLoadBalancerUpdate> lastLoadBalancerUpdate,
      @JsonProperty("currentDeployState") DeployState currentDeployState, @JsonProperty("deployProgress") Optional<SingularityDeployProgress> deployProgress, @JsonProperty("updatedRequest") Optional<SingularityRequest> updatedRequest) {
    this.deployMarker = deployMarker;
    this.lastLoadBalancerUpdate = lastLoadBalancerUpdate;
    this.currentDeployState = currentDeployState;
    this.deployProgress = deployProgress;
    this.updatedRequest = updatedRequest;
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

  public Optional<SingularityDeployProgress> getDeployProgress() {
    return deployProgress;
  }

  public Optional<SingularityRequest> getUpdatedRequest() {
    return updatedRequest;
  }

  @Override
  public String toString() {
    return "SingularityPendingDeploy{" +
        "deployMarker=" + deployMarker +
        ", lastLoadBalancerUpdate=" + lastLoadBalancerUpdate +
        ", currentDeployState=" + currentDeployState +
        ", deployProgress=" + deployProgress +
        ", updatedRequest=" + updatedRequest +
        '}';
  }
}
