package com.hubspot.singularity;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SingularitySlave extends SingularityMachineAbstraction {

  private final String host;
  private final String rackId;

  public static SingularitySlave fromBytes(byte[] bytes, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(bytes, SingularitySlave.class);
    } catch (IOException e) {
      throw new SingularityJsonException(e);
    }
  }
  
  @JsonCreator
  public SingularitySlave(@JsonProperty("slaveId") String slaveId, @JsonProperty("host") String host, @JsonProperty("rackId") String rackId, @JsonProperty("state") SingularityMachineState state) {
    super(slaveId, state);
    this.host = host;
    this.rackId = rackId;
  }
  
  public String getHost() {
    return host;
  }

  public String getRackId() {
    return rackId;
  }

  @Override
  public String toString() {
    return "SingularitySlave [slaveId=" + getId() + ", host=" + host + ", rackId=" + rackId + ", state=" + getState() + "]";
  }
  
}
