package com.hubspot.singularity;

public enum MachineState {

  MISSING_ON_STARTUP(false, false), ACTIVE(false, false), STARTING_DECOMMISSION(true, true), DECOMMISSIONING(true, true), DECOMMISSIONED(true, true), DEAD(false, false), FROZEN(false, true);

  private final boolean decommissioning;
  private final boolean frozen;

  MachineState(boolean decommissioning, boolean frozen) {
    this.decommissioning = decommissioning;
    this.frozen = frozen;
  }

  public boolean isDecommissioning() {
    return decommissioning;
  }

  public boolean isFrozen() {
    return frozen;
  }
}
