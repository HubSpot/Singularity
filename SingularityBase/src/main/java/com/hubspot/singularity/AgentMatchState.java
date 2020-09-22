package com.hubspot.singularity;

public enum AgentMatchState {
  OK(true),
  PREFERRED_AGENT(true),
  NOT_RACK_OR_AGENT_PARTICULAR(true),
  RESOURCES_DO_NOT_MATCH(false),
  RACK_SATURATED(false),
  AGENT_SATURATED(false),
  AGENT_DECOMMISSIONING(false),
  RACK_DECOMMISSIONING(false),
  RACK_AFFINITY_NOT_MATCHING(false),
  AGENT_ATTRIBUTES_DO_NOT_MATCH(false),
  AGENT_FROZEN(false),
  RACK_FROZEN(false);

  private final boolean isMatchAllowed;

  private AgentMatchState(boolean isMatchAllowed) {
    this.isMatchAllowed = isMatchAllowed;
  }

  public boolean isMatchAllowed() {
    return isMatchAllowed;
  }
}
