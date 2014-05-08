package com.hubspot.singularity;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

public class SingularityLoadBalancerUpdate extends SingularityJsonObject {

  private final LoadBalancerState loadBalancerState;
  private final String loadBalancerRequestId;
  private final Optional<String> message;
  private final long timestamp;
  
  public static SingularityLoadBalancerUpdate fromBytes(byte[] bytes, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(bytes, SingularityLoadBalancerUpdate.class);
    } catch (IOException e) {
      throw new SingularityJsonException(e);
    }
  }
  
  @JsonCreator
  public SingularityLoadBalancerUpdate(@JsonProperty("state") LoadBalancerState loadBalancerState, @JsonProperty("loadBalancerRequestId") String loadBalancerRequestId, @JsonProperty("message") Optional<String> message, 
      @JsonProperty("timestamp") long timestamp) {
    this.loadBalancerState = loadBalancerState;
    this.message = message;
    this.loadBalancerRequestId = loadBalancerRequestId;
    this.timestamp = timestamp;
  }

  public String getLoadBalancerRequestId() {
    return loadBalancerRequestId;
  }

  public LoadBalancerState getLoadBalancerState() {
    return loadBalancerState;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public Optional<String> getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return "SingularityLoadBalancerUpdate [loadBalancerState=" + loadBalancerState + ", loadBalancerRequestId=" + loadBalancerRequestId + ", message=" + message + ", timestamp=" + timestamp + "]";
  }
  
}
