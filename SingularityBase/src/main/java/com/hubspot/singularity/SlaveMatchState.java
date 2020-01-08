package com.hubspot.singularity;

public enum SlaveMatchState {
  OK(true),
  NOT_RACK_OR_SLAVE_PARTICULAR(true),
  RESOURCES_DO_NOT_MATCH(false),
  RACK_SATURATED(false),
  SLAVE_SATURATED(false),
  SLAVE_DECOMMISSIONING(false),
  RACK_DECOMMISSIONING(false),
  RACK_AFFINITY_NOT_MATCHING(false),
  SLAVE_ATTRIBUTES_DO_NOT_MATCH(false),
  SLAVE_FROZEN(false),
  RACK_FROZEN(false);

  private final boolean isMatchAllowed;

  private SlaveMatchState(boolean isMatchAllowed) {
    this.isMatchAllowed = isMatchAllowed;
  }

  public boolean isMatchAllowed() {
    return isMatchAllowed;
  }

}
