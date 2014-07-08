package com.hubspot.singularity;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

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
  
  public SingularitySlave(String slaveId, String host, String rackId) {
    super(slaveId);
    this.host = host;
    this.rackId = rackId;
  }
 
  @JsonCreator
  public SingularitySlave(@JsonProperty("slaveId") String slaveId, @JsonProperty("host") String host, @JsonProperty("rackId") String rackId, @JsonProperty("state") SingularityMachineState state, 
      @JsonProperty("firstSeenAt") long firstSeenAt, @JsonProperty("decomissioningBy") Optional<String> decomissioningBy, @JsonProperty("decomissioningAt") Optional<Long> decomissioningAt, 
      @JsonProperty("decomissionedAt") Optional<Long> decomissionedAt, @JsonProperty("deadAt") Optional<Long> deadAt) {
    super(slaveId, state, firstSeenAt, decomissioningBy, decomissioningAt, decomissionedAt, deadAt);
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
    return "SingularitySlave [host=" + host + ", rackId=" + rackId + ", getDecomissioningBy()=" + getDecomissioningBy() + ", getDecomissioningAt()=" + getDecomissioningAt() + ", getId()=" + getId() + ", getDeadAt()=" + getDeadAt()
        + ", getDecomissionedAt()=" + getDecomissionedAt() + ", getFirstSeenAt()=" + getFirstSeenAt() + ", getState()=" + getState() + "]";
  }
  
}
