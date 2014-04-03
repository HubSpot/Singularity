package com.hubspot.singularity;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SingularityLoadBalancerResponse extends SingularityJsonObject {

  private final String loadBalancerRequestId;
  private final LoadBalancerState loadBalancerState;
  
  public static SingularityLoadBalancerResponse fromBytes(byte[] bytes, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(bytes, SingularityLoadBalancerResponse.class);
    } catch (IOException e) {
      throw new SingularityJsonException(e);
    }
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
