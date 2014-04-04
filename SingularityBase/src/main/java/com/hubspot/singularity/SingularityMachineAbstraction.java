package com.hubspot.singularity;


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
  
  public SingularityMachineState getState() {
    return state;
  }
  
  public void setState(SingularityMachineState state) {
    this.state = state;
  }
  
  
}
