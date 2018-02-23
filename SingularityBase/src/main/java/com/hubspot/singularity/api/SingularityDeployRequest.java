package com.hubspot.singularity.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityRequest;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(title = "Describes a new deploy")
public class SingularityDeployRequest {

  private final Optional<Boolean> unpauseOnSuccessfulDeploy;
  private final SingularityDeploy deploy;
  private final Optional<String> message;
  private final Optional<SingularityRequest> updatedRequest;

  @JsonCreator
  public SingularityDeployRequest(
      @JsonProperty("deploy") SingularityDeploy deploy,
      @JsonProperty("unpauseOnSuccessfulDeploy") Optional<Boolean> unpauseOnSuccessfulDeploy,
      @JsonProperty("message") Optional<String> message,
      @JsonProperty("updatedRequest") Optional<SingularityRequest> updatedRequest) {
    this.deploy = deploy;
    this.unpauseOnSuccessfulDeploy = unpauseOnSuccessfulDeploy;
    this.message = message;
    this.updatedRequest = updatedRequest;
  }

  public SingularityDeployRequest(SingularityDeploy deploy, Optional<Boolean> unpauseOnSuccessfulDeploy, Optional<String> message) {
    this(deploy, unpauseOnSuccessfulDeploy, message, Optional.<SingularityRequest>absent());
  }

  @Schema(title = "If deploy is successful, also unpause the request", nullable = true, defaultValue = "false")
  public Optional<Boolean> getUnpauseOnSuccessfulDeploy() {
    return unpauseOnSuccessfulDeploy;
  }

  @Schema(required = true, title = "The Singularity deploy object, containing all the required details about the Deploy")
  public SingularityDeploy getDeploy() {
    return deploy;
  }

  @Schema(title = "A message to show users about this deploy (metadata)", nullable = true)
  public Optional<String> getMessage() {
    return message;
  }

  @Schema(title = "use this request data for this deploy, and update the request on successful deploy", nullable = true)
  public Optional<SingularityRequest> getUpdatedRequest() {
    return updatedRequest;
  }

  @JsonIgnore
  public boolean isUnpauseOnSuccessfulDeploy() {
    return unpauseOnSuccessfulDeploy.or(Boolean.FALSE);
  }

  @Override
  public String toString() {
    return "SingularityDeployRequest{" +
        "unpauseOnSuccessfulDeploy=" + unpauseOnSuccessfulDeploy +
        ", deploy=" + deploy +
        ", message=" + message +
        ", updatedRequest=" + updatedRequest +
        '}';
  }
}
