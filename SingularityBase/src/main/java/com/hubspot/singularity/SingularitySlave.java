package com.hubspot.singularity;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Singularity's view of a Mesos slave")
public class SingularitySlave extends SingularityMachineAbstraction<SingularitySlave> {

  private final String host;
  private final String rackId;
  private final Map<String, String> attributes;

  public SingularitySlave(String slaveId, String host, String rackId, Map<String, String> attributes) {
    super(slaveId);

    this.host = host;
    this.rackId = rackId;
    this.attributes = attributes;
  }

  @JsonCreator
  public SingularitySlave(@JsonProperty("slaveId") String slaveId, @JsonProperty("firstSeenAt") long firstSeenAt, @JsonProperty("currentState") SingularityMachineStateHistoryUpdate currentState,
      @JsonProperty("host") String host, @JsonProperty("rackId") String rackId, @JsonProperty("attributes") Map<String, String> attributes) {
    super(slaveId, firstSeenAt, currentState);
    this.host = host;
    this.rackId = rackId;
    this.attributes = attributes;
  }

  @Override
  public SingularitySlave changeState(SingularityMachineStateHistoryUpdate newState) {
    return new SingularitySlave(getId(), getFirstSeenAt(), newState, host, rackId, attributes);
  }

  @ApiModelProperty("Slave hostname")
  public String getHost() {
    return host;
  }

  @JsonIgnore
  @Override
  public String getName() {
    return String.format("%s (%s)", getHost(), getId());
  }

  @JsonIgnore
  @Override
  public String getTypeName() {
    return "Slave";
  }

  @ApiModelProperty("Slave rack ID")
  public String getRackId() {
    return rackId;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  @Override
  public String toString() {
    return "SingularitySlave [host=" + host + ", rackId=" + rackId + ", getId()=" + getId() + ", getFirstSeenAt()=" + getFirstSeenAt() + ", getCurrentState()=" + getCurrentState() + "]";
  }

}
