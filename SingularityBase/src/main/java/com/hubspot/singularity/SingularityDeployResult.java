package com.hubspot.singularity;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

public class SingularityDeployResult extends SingularityJsonObject {

  private final DeployState deployState;
  private final Optional<SingularityLoadBalancerUpdate> lbUpdate;
  private final Optional<String> message;
  private final long timestamp;

  public static SingularityDeployResult fromBytes(byte[] bytes, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(bytes, SingularityDeployResult.class);
    } catch (IOException e) {
      throw new SingularityJsonException(e);
    }
  }

  public SingularityDeployResult(DeployState deployState) {
    this(deployState, Optional.<String> absent(), Optional.<SingularityLoadBalancerUpdate> absent(), System.currentTimeMillis());
  }
  
  public SingularityDeployResult(DeployState deployState, SingularityLoadBalancerUpdate lbUpdate) {
    this(deployState, Optional.of(String.format("Load balancer had state %s%s", lbUpdate.getLoadBalancerState(), lbUpdate.getMessage().isPresent() && lbUpdate.getMessage().get().length() > 0 ? String.format(" (%s)", lbUpdate.getMessage().get()) : "")), Optional.of(lbUpdate), System.currentTimeMillis());
  }
  
  public SingularityDeployResult(DeployState deployState, String message) {
    this(deployState, Optional.of(message), Optional.<SingularityLoadBalancerUpdate> absent(), System.currentTimeMillis());
  }
  
  @JsonCreator
  public SingularityDeployResult(@JsonProperty("deployState") DeployState deployState, @JsonProperty("message") Optional<String> message, @JsonProperty("lbUpdate") Optional<SingularityLoadBalancerUpdate> lbUpdate, 
      @JsonProperty("timestamp") long timestamp) {
    this.deployState = deployState;
    this.lbUpdate = lbUpdate;
    this.message = message;
    this.timestamp = timestamp;
  }
  
  public Optional<SingularityLoadBalancerUpdate> getLbUpdate() {
    return lbUpdate;
  }

  public Optional<String> getMessage() {
    return message;
  }

  public DeployState getDeployState() {
    return deployState;
  }

  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public String toString() {
    return "SingularityDeployResult [deployState=" + deployState + ", lbUpdate=" + lbUpdate + ", message=" + message + ", timestamp=" + timestamp + "]";
  }

}
