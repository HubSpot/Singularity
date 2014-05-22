package com.hubspot.singularity;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

public class SingularityLoadBalancerResponse extends SingularityJsonObject {

  private final String loadBalancerRequestId;
  private final LoadBalancerState loadBalancerState;
  private final Optional<String> message;
  
  public static SingularityLoadBalancerResponse fromBytes(byte[] bytes, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(bytes, SingularityLoadBalancerResponse.class);
    } catch (IOException e) {
      throw new SingularityJsonException(e);
    }
  }
  
  @JsonCreator
  public SingularityLoadBalancerResponse(@JsonProperty("loadBalancerRequestId") String loadBalancerRequestId, @JsonProperty("loadBalancerState") LoadBalancerState loadBalancerState, @JsonProperty("message") Optional<String> message) {
    this.loadBalancerRequestId = loadBalancerRequestId;
    this.loadBalancerState = loadBalancerState;
    this.message = message;
  }

  public String getLoadBalancerRequestId() {
    return loadBalancerRequestId;
  }

  public LoadBalancerState getLoadBalancerState() {
    return loadBalancerState;
  }

  public Optional<String> getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return "SingularityLoadBalancerResponse [" +
        "loadBalancerRequestId='" + loadBalancerRequestId + '\'' +
        ", loadBalancerState=" + loadBalancerState +
        ", message=" + message +
        ']';
  }
  
}
