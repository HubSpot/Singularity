package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityRack extends SingularityMachineAbstraction<SingularityRack> {

  public SingularityRack(String rackId) {
    super(rackId);
  }

  @JsonCreator
  public SingularityRack(@JsonProperty("rackId") String rackId, @JsonProperty("firstSeenAt") long firstSeenAt,
      @JsonProperty("currentState") SingularityMachineStateHistoryUpdate currentState) {
    super(rackId, firstSeenAt, currentState);
  }

  @Override
  public SingularityRack changeState(SingularityMachineStateHistoryUpdate newState) {
    return new SingularityRack(getId(), getFirstSeenAt(), newState);
  }

  @JsonIgnore
  @Override
  public String getName() {
    return getId();
  }

  @JsonIgnore
  @Override
  public String getTypeName() {
    return "rack";
  }

  @Override
  public String toString() {
    return "SingularityRack [getId()=" + getId() + ", getFirstSeenAt()=" + getFirstSeenAt() + ", getCurrentState()=" + getCurrentState() + "]";
  }

}
