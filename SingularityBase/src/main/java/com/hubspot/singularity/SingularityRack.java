package com.hubspot.singularity;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SingularityRack extends SingularityMachineAbstraction {

  public static SingularityRack fromBytes(byte[] bytes, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(bytes, SingularityRack.class);
    } catch (IOException e) {
      throw new SingularityJsonException(e);
    }
  }

  @JsonCreator
  public SingularityRack(@JsonProperty("rackId") String rackId, @JsonProperty("state") SingularityMachineState state) {
    super(rackId, state);
  }
  
  @Override
  public String toString() {
    return "SingularityRack [getId()=" + getId() + ", getState()=" + getState() + "]";
  }
  
}
