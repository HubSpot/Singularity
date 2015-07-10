package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityRack extends SingularityMachineAbstraction<SingularityRack> {

  private final String name;

  public SingularityRack(String rackId, String name) {
    super(rackId);

    this.name = name;
  }

  @JsonCreator
  public SingularityRack(@JsonProperty("rackId") String rackId, @JsonProperty("firstSeenAt") long firstSeenAt,
      @JsonProperty("currentState") SingularityMachineStateHistoryUpdate currentState, @JsonProperty("name") String name) {
    super(rackId, firstSeenAt, currentState);

    this.name = name;
  }

  @Override
  public SingularityRack changeState(SingularityMachineStateHistoryUpdate newState) {
    return new SingularityRack(getId(), getFirstSeenAt(), newState, getName());
  }

  @JsonIgnore
  @Override
  public String getName() {
    return name;
  }

  @JsonIgnore
  @Override
  public String getTypeName() {
    return "rack";
  }

  @Override
  public String toString() {
    return "SingularityRack [name=" + name + ", getId()=" + getId() + ", getFirstSeenAt()=" + getFirstSeenAt() + ", getCurrentState()=" + getCurrentState() + "]";
  }

}
