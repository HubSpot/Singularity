package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Represents Singularity's view of a Mesos slave")
public class SingularitySlave extends SingularityMachineAbstraction {

  private final String host;
  private final String rackId;

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

  @ApiModelProperty("Slave hostname")
  public String getHost() {
    return host;
  }

  @ApiModelProperty("Slave rack ID")
  public String getRackId() {
    return rackId;
  }

  @Override
  public String toString() {
    return "SingularitySlave [host=" + host + ", rackId=" + rackId + ", getDecomissioningBy()=" + getDecomissioningBy() + ", getDecomissioningAt()=" + getDecomissioningAt() + ", getId()=" + getId() + ", getDeadAt()=" + getDeadAt()
        + ", getDecomissionedAt()=" + getDecomissionedAt() + ", getFirstSeenAt()=" + getFirstSeenAt() + ", getState()=" + getState() + "]";
  }

}
