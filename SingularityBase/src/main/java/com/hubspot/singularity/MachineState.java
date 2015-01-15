package com.hubspot.singularity;

public enum MachineState {

  MISSING_ON_STARTUP(false), ACTIVE(false), STARTING_DECOMMISSION(true), DECOMMISSIONING(true), DECOMMISSIONED(true), DEAD(false);

  private final boolean isDecommissioning;

  private MachineState(boolean isDecommissioning) {
    this.isDecommissioning = isDecommissioning;
  }

  public boolean isDecommissioning() {
    return isDecommissioning;
  }


}
