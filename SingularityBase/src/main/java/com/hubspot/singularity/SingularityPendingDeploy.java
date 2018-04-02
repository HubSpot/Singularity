package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Describes an in-progress deploy")
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

  @Schema(description = "Uniquely identifies this deploy")
  public SingularityDeployMarker getDeployMarker() {
    return deployMarker;
  }

  @Schema(description = "The latest load balancer update for this deploy (if a long running service with load balancing enabled)", nullable = true)
  public Optional<SingularityLoadBalancerUpdate> getLastLoadBalancerUpdate() {
    return lastLoadBalancerUpdate;
  }

  @Schema(description = "Current state of this deploy")
  public DeployState getCurrentDeployState() {
    return currentDeployState;
  }

  @Schema(description = "Describes the progress this deploy has made so far", nullable = true)
  public Optional<SingularityDeployProgress> getDeployProgress() {
    return deployProgress;
  }

  @Schema(description = "New request data to be committed if this deploy succeeds")
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
