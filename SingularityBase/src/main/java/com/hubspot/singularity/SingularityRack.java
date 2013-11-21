package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SingularityRack extends SingularityMachineAbstraction {

  public SingularityRack(String rackId, SingularityMachineState state) {
    super(rackId, state);
  }
  
  @JsonCreator
  public SingularityRack(@JsonProperty("rackId") String rackId, @JsonProperty("state") String state) {
    this(rackId, SingularityMachineState.valueOf(state));
  }
  
  public static SingularityRack fromBytes(byte[] bytes, ObjectMapper objectMapper) throws Exception {
    return objectMapper.readValue(bytes, SingularityRack.class);
  }

  @Override
  public String toString() {
    return "SingularityRack [getId()=" + getId() + ", getState()=" + getState() + "]";
  }
  
}
