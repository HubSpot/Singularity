package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;


public class SingularityRequestParent extends SingularityJsonObject {

  private final SingularityRequest request;
  private final Optional<SingularityRequestDeployState> deployState;
  private final Optional<SingularityDeploy> activeDeploy;
  private final Optional<SingularityDeploy> pendingDeploy;
  
  @JsonCreator
  public SingularityRequestParent(@JsonProperty("request") SingularityRequest request, @JsonProperty("deployState") Optional<SingularityRequestDeployState> deployState, @JsonProperty("activeDeploy") Optional<SingularityDeploy> activeDeploy, @JsonProperty("pendingDeploy") Optional<SingularityDeploy> pendingDeploy) {
    this.request = request;
    this.deployState = deployState;
    this.activeDeploy = activeDeploy;
    this.pendingDeploy = pendingDeploy;
  }
 
  public SingularityRequest getRequest() {
    return request;
  }

  public Optional<SingularityRequestDeployState> getDeployState() {
    return deployState;
  }

  public Optional<SingularityDeploy> getActiveDeploy() {
    return activeDeploy;
  }

  public Optional<SingularityDeploy> getPendingDeploy() {
    return pendingDeploy;
  }

}
