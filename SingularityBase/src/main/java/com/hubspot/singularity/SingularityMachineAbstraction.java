package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class SingularityMachineAbstraction extends SingularityJsonObject {
  
  private final String id;
  
  private SingularityMachineState state;
  
  public enum SingularityMachineState {
    ACTIVE, DECOMISSIONING, DECOMISSIONED, DEAD, 
  }
  
  public SingularityMachineAbstraction(String id, SingularityMachineState state) {
    this.id = id;
    this.state = state;
  }
  
  public String getId() {
    return id;
  }

  @JsonIgnore
  public SingularityMachineState getStateEnum() {
    return state;
  }
  
  public String getState() {
    return state.name();
  }
  
  public void setState(SingularityMachineState state) {
    this.state = state;
  }
  
}
