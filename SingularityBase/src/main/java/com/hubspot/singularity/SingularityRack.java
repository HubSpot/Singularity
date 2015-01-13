package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityRack extends SingularityMachineAbstraction {

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
