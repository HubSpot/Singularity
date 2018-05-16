package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A webhook event representing an update to deploy data")
public class SingularityDeployUpdate {

  @Schema
  public enum DeployEventType {
    STARTING, FINISHED;
  }

  private final SingularityDeployMarker deployMarker;
  private final Optional<SingularityDeploy> deploy;
  private final DeployEventType eventType;
  private final Optional<SingularityDeployResult> deployResult;

  @JsonCreator
  public SingularityDeployUpdate(@JsonProperty("deployMarker") SingularityDeployMarker deployMarker, @JsonProperty("deploy") Optional<SingularityDeploy> deploy, @JsonProperty("eventType") DeployEventType eventType, @JsonProperty("deployResult") Optional<SingularityDeployResult> deployResult) {
    this.deployMarker = deployMarker;
    this.deploy = deploy;
    this.eventType = eventType;
    this.deployResult = deployResult;
  }

  @Schema(description = "An object identifying a particular deploy for a request")
  public SingularityDeployMarker getDeployMarker() {
    return deployMarker;
  }

  @Schema(description = "The full data of the deploy", nullable = true)
  public Optional<SingularityDeploy> getDeploy() {
    return deploy;
  }

  @Schema(description = "Starting or Finished")
  public DeployEventType getEventType() {
    return eventType;
  }

  @Schema(description = "The result of the deploy if it has finished", nullable = true)
  public Optional<SingularityDeployResult> getDeployResult() {
    return deployResult;
  }

  @Override
  public String toString() {
    return "SingularityDeployUpdate{" +
        "deployMarker=" + deployMarker +
        ", deploy=" + deploy +
        ", eventType=" + eventType +
        ", deployResult=" + deployResult +
        '}';
  }
}
