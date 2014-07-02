package com.hubspot.singularity;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

public class SingularityRack extends SingularityMachineAbstraction {

  public static SingularityRack fromBytes(byte[] bytes, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(bytes, SingularityRack.class);
    } catch (IOException e) {
      throw new SingularityJsonException(e);
    }
  }

  public SingularityRack(String rackId) {
    super(rackId);
  }
  
  @JsonCreator
  public SingularityRack(@JsonProperty("rackId") String rackId, @JsonProperty("state") SingularityMachineState state,
      @JsonProperty("firstSeenAt") long firstSeenAt, @JsonProperty("decomissioningBy") Optional<String> decomissioningBy, @JsonProperty("decomissioningAt") Optional<Long> decomissioningAt, 
      @JsonProperty("decomissionedAt") Optional<Long> decomissionedAt, @JsonProperty("deadAt") Optional<Long> deadAt) {
    super(rackId, state, firstSeenAt, decomissioningBy, decomissioningAt, decomissionedAt, deadAt);
  }

  @Override
  public String toString() {
    return "SingularityRack [getDecomissioningBy()=" + getDecomissioningBy() + ", getDecomissioningAt()=" + getDecomissioningAt() + ", getId()=" + getId() + ", getDeadAt()=" + getDeadAt() + ", getDecomissionedAt()=" + getDecomissionedAt()
        + ", getFirstSeenAt()=" + getFirstSeenAt() + ", getState()=" + getState() + "]";
  }
  
}
