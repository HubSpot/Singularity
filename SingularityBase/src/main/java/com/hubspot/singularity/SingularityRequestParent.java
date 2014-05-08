package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;


public class SingularityRequestParent extends SingularityJsonObject {

  private final SingularityRequest request;
  private final Optional<SingularityRequestDeployState> requestDeployState;
  private final Optional<SingularityDeploy> activeDeploy;
  private final Optional<SingularityDeploy> pendingDeploy;
  private final Optional<SingularityPendingDeploy> pendingDeployState;
  
  @JsonCreator
  public SingularityRequestParent(@JsonProperty("request") SingularityRequest request, @JsonProperty("requestDeployState") Optional<SingularityRequestDeployState> requestDeployState, @JsonProperty("activeDeploy") Optional<SingularityDeploy> activeDeploy, 
      @JsonProperty("pendingDeploy") Optional<SingularityDeploy> pendingDeploy, @JsonProperty("pendingDeployState") Optional<SingularityPendingDeploy> pendingDeployState) {
    this.request = request;
    this.requestDeployState = requestDeployState;
    this.activeDeploy = activeDeploy;
    this.pendingDeploy = pendingDeploy;
    this.pendingDeployState = pendingDeployState;
  }
 
  public SingularityRequest getRequest() {
    return request;
  }

  public Optional<SingularityRequestDeployState> getRequestDeployState() {
    return requestDeployState;
  }

  public Optional<SingularityDeploy> getActiveDeploy() {
    return activeDeploy;
  }

  public Optional<SingularityDeploy> getPendingDeploy() {
    return pendingDeploy;
  }
  
  public Optional<SingularityPendingDeploy> getPendingDeployState() {
    return pendingDeployState;
  }

  @Override
  public String toString() {
    return "SingularityRequestParent [request=" + request + ", requestDeployState=" + requestDeployState + ", activeDeploy=" + activeDeploy + ", pendingDeploy=" + pendingDeploy + ", pendingDeployState=" + pendingDeployState + "]";
  }
  
}
