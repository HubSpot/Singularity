package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SingularitySlave extends SingularityJsonObject {

  private final String slaveId;
  private final String host;
  private final String rackId;
  
  @JsonCreator
  public SingularitySlave(@JsonProperty("slaveId") String slaveId, @JsonProperty("host") String host, @JsonProperty("rackId") String rackId) {
    this.slaveId = slaveId;
    this.host = host;
    this.rackId = rackId;
  }

  public String getSlaveId() {
    return slaveId;
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
    return "SingularitySlave [slaveId=" + slaveId + ", host=" + host + ", rackId=" + rackId + "]";
  }
  
}
