package com.hubspot.singularity;

public enum MachineState {

  MISSING_ON_STARTUP(false), ACTIVE(false), STARTING_DECOMISSION(true), DECOMISSIONING(true), DECOMISSIONED(true), DEAD(false);

  private final boolean isDecomissioning;

  private MachineState(boolean isDecomissioning) {
    this.isDecomissioning = isDecomissioning;
  }

  public boolean isDecomissioning() {
    return isDecomissioning;
  }


}
