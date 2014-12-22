package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityRack extends SingularityMachineAbstraction<SingularityRack> {

  public SingularityRack(String rackId) {
    super(rackId);
  }

  @JsonCreator
  public SingularityRack(@JsonProperty("rackId") String rackId, @JsonProperty("firstSeenAt") long firstSeenAt, @JsonProperty("currentState") SingularityMachineStateHistoryUpdate currentState) {
    super(rackId, firstSeenAt, currentState);
  }

  @Override
  public SingularityRack changeState(SingularityMachineStateHistoryUpdate newState) {
    return new SingularityRack(getId(), getFirstSeenAt(), newState);
  }

}
