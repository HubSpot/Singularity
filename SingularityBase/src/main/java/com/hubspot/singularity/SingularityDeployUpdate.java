package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityDeployUpdate {

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

  public SingularityDeployMarker getDeployMarker() {
    return deployMarker;
  }

  public Optional<SingularityDeploy> getDeploy() {
    return deploy;
  }

  public DeployEventType getEventType() {
    return eventType;
  }

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
