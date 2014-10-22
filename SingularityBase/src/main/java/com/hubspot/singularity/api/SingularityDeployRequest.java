package com.hubspot.singularity.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityDeploy;

public class SingularityDeployRequest {

  private final Optional<String> user;
  private final Optional<Boolean> unpauseOnSuccessfulDeploy;
  private final SingularityDeploy deploy;

  @JsonCreator
  public SingularityDeployRequest(@JsonProperty("deploy") SingularityDeploy deploy, @JsonProperty("user") Optional<String> user, @JsonProperty("unpauseOnSuccessfulDeploy") Optional<Boolean> unpauseOnSuccessfulDeploy) {
    this.deploy = deploy;
    this.user = user;
    this.unpauseOnSuccessfulDeploy = unpauseOnSuccessfulDeploy;
  }

  public Optional<String> getUser() {
    return user;
  }

  public Optional<Boolean> getUnpauseOnSuccessfulDeploy() {
    return unpauseOnSuccessfulDeploy;
  }

  public SingularityDeploy getDeploy() {
    return deploy;
  }

  @Override
  public String toString() {
    return "SingularityDeployRequest [user=" + user + ", unpauseOnSuccessfulDeploy=" + unpauseOnSuccessfulDeploy + ", deploy=" + deploy + "]";
  }

}
