package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SingularitySlave extends SingularityMachineAbstraction {

  private final String host;
  private final String rackId;
  
  public SingularitySlave(String slaveId, String host, String rackId, SingularityMachineState state) {
    super(slaveId, state);
    this.host = host;
    this.rackId = rackId;
  }
  
  @JsonCreator
  public SingularitySlave(@JsonProperty("slaveId") String slaveId, @JsonProperty("host") String host, @JsonProperty("rackId") String rackId, @JsonProperty("state") String state) {
    this(slaveId, host, rackId, SingularityMachineState.valueOf(state));
  }
  
  public String getHost() {
    return host;
  }

  public String getRackId() {
    return rackId;
  }

  public static SingularitySlave fromBytes(byte[] bytes, ObjectMapper objectMapper) throws Exception {
    return objectMapper.readValue(bytes, SingularitySlave.class);
  }

  @Override
  public String toString() {
    return "SingularitySlave [slaveId=" + getId() + ", host=" + host + ", rackId=" + rackId + ", state=" + getState() + "]";
  }
  
}
