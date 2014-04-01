package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SingularityLoadBalancerResponse extends SingularityJsonObject {

  private final String loadBalancerRequestId;
  private final LoadBalancerState loadBalancerState;
  
  public static SingularityLoadBalancerResponse fromBytes(byte[] bytes, ObjectMapper objectMapper) throws Exception {
    return objectMapper.readValue(bytes, SingularityLoadBalancerResponse.class);
  }
  
  @JsonCreator
  public SingularityLoadBalancerResponse(@JsonProperty("loadBalancerRequestId") String loadBalancerRequestId, @JsonProperty("loadBalancerState") LoadBalancerState loadBalancerState) {
    this.loadBalancerRequestId = loadBalancerRequestId;
    this.loadBalancerState = loadBalancerState;
  }

  public String getLoadBalancerRequestId() {
    return loadBalancerRequestId;
  }

  public LoadBalancerState getLoadBalancerState() {
    return loadBalancerState;
  }

  @Override
  public String toString() {
    return "SingularityLoadBalancerResponse [loadBalancerRequestId=" + loadBalancerRequestId + ", loadBalancerState=" + loadBalancerState + "]";
  }
    
}
