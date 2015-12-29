package com.hubspot.singularity.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityDeploy;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class SingularityDeployRequest {

  private final Optional<Boolean> unpauseOnSuccessfulDeploy;
  private final SingularityDeploy deploy;
  private final Optional<String> message;

  @JsonCreator
  public SingularityDeployRequest(
      @JsonProperty("deploy") SingularityDeploy deploy,
      @JsonProperty("unpauseOnSuccessfulDeploy") Optional<Boolean> unpauseOnSuccessfulDeploy,
      @JsonProperty("message") Optional<String> message) {
    this.deploy = deploy;
    this.unpauseOnSuccessfulDeploy = unpauseOnSuccessfulDeploy;
    this.message = message;
  }

  @ApiModelProperty(required=false, value="If deploy is successful, also unpause the request.")
  public Optional<Boolean> getUnpauseOnSuccessfulDeploy() {
    return unpauseOnSuccessfulDeploy;
  }

  @JsonIgnore
  public boolean isUnpauseOnSuccessfulDeploy() {
    return unpauseOnSuccessfulDeploy.or(Boolean.FALSE);
  }

  @ApiModelProperty(required=true, value="The Singularity deploy object")
  public SingularityDeploy getDeploy() {
    return deploy;
  }

  public Optional<String> getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return "SingularityDeployRequest [unpauseOnSuccessfulDeploy=" + unpauseOnSuccessfulDeploy + ", deploy=" + deploy + ", message=" + message + "]";
  }

}
