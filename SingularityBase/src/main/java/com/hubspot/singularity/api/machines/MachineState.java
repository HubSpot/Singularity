package com.hubspot.singularity.api.machines;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public enum MachineState {

  MISSING_ON_STARTUP(false, true), ACTIVE(false, false), STARTING_DECOMMISSION(true, false), DECOMMISSIONING(true, false), DECOMMISSIONED(true, false), DEAD(false, true), FROZEN(false, false);

  private final boolean decommissioning;
  private final boolean inactive;

  MachineState(boolean decommissioning, boolean inactive) {
    this.decommissioning = decommissioning;
    this.inactive = inactive;
  }

  public boolean isDecommissioning() {
    return decommissioning;
  }

  public boolean isInactive() {
    return inactive;
  }
}
