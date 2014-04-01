package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

public class SingularityPendingDeploy extends SingularityJsonObject {

  private final SingularityDeployMarker deployMarker;
  private final Optional<LoadBalancerState> loadBalancerState;
  
  public enum LoadBalancerState {
    FAILED, WAITING, SUCCESS, CANCELING, CANCELED; 
  }

  public static SingularityPendingDeploy fromBytes(byte[] bytes, ObjectMapper objectMapper) throws Exception {
    return objectMapper.readValue(bytes, SingularityPendingDeploy.class);
  }

  @JsonCreator
  public SingularityPendingDeploy(@JsonProperty("deployMarker") SingularityDeployMarker deployMarker, @JsonProperty("loadBalancerState") Optional<LoadBalancerState> loadBalancerState) {
    this.deployMarker = deployMarker;
    this.loadBalancerState = loadBalancerState;
  }

  public SingularityDeployMarker getDeployMarker() {
    return deployMarker;
  }

  public Optional<LoadBalancerState> getLoadBalancerState() {
    return loadBalancerState;
  }

  @Override
  public String toString() {
    return "SingularityPendingDeploy [deployMarker=" + deployMarker + ", loadBalancerState=" + loadBalancerState + "]";
  }
 
}
