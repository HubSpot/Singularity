package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SingularityLoadBalancerResponse extends SingularityJsonObject {

  private final String loadBalancerRequestId;
  private final LoadBalancerState loadBalancerState;
  
  public static SingularityLoadBalancerResponse fromBytes(byte[] bytes, ObjectMapper objectMapper) throws Exception {
    return objectMapper.readValue(bytes, SingularityLoadBalancerResponse.class);
  }
  
  @JsonCreator
  public SingularityLoadBalancerResponse(@JsonProperty("loadBalancerRequestId") String loadBalancerRequestId, @JsonProperty("loadBalancerState") String loadBalancerState) {
    this(loadBalancerRequestId, LoadBalancerState.valueOf(loadBalancerRequestId));
  }
  
  public SingularityLoadBalancerResponse(String loadBalancerRequestId, LoadBalancerState loadBalancerState) {
    this.loadBalancerRequestId = loadBalancerRequestId;
    this.loadBalancerState = loadBalancerState;
  }

  public String getLoadBalancerRequestId() {
    return loadBalancerRequestId;
  }

  @JsonIgnore
  public LoadBalancerState getLoadBalancerStateEnum() {
    return loadBalancerState;
  }
  
  public String getLoadBalancerState() {
    return loadBalancerState.name();
  }

  @Override
  public String toString() {
    return "SingularityLoadBalancerResponse [loadBalancerRequestId=" + loadBalancerRequestId + ", loadBalancerState=" + loadBalancerState + "]";
  }
    
}
