package com.hubspot.singularity;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SingularityDeployState extends SingularityJsonObject {

  private final DeployState deployState;
  private final long timestamp;

  public static SingularityDeployState fromBytes(byte[] bytes, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(bytes, SingularityDeployState.class);
    } catch (IOException e) {
      throw new SingularityJsonException(e);
    }
  }

  @JsonCreator
  public SingularityDeployState(@JsonProperty("deployState") DeployState deployState, @JsonProperty("timestamp") long timestamp) {
    this.deployState = deployState;
    this.timestamp = timestamp;
  }

  public DeployState getDeployState() {
    return deployState;
  }

  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public String toString() {
    return "SingularityDeployState [deployState=" + deployState + ", timestamp=" + timestamp + "]";
  }  
  
}
