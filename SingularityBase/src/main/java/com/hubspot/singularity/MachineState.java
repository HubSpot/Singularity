package com.hubspot.singularity;

public enum MachineState {

  MISSING_ON_STARTUP(false), ACTIVE(false), STARTING_DECOMMISSION(true), DECOMMISSIONING(true), DECOMMISSIONED(true), DEAD(false), FROZEN(false);

  private final boolean decommissioning;

  MachineState(boolean decommissioning) {
    this.decommissioning = decommissioning;
  }

  public boolean isDecommissioning() {
    return decommissioning;
  }
}
