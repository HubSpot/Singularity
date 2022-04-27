package com.hubspot.singularity.config;

import com.hubspot.singularity.AgentPlacement;
import java.util.Optional;

/**
 * Stores temporary overrides for certain Singularity behavior,
 * because calling configuration setters directly doesn't work.
 */
public class OverrideConfiguration {

  /** If false, ignore rack sensitive requests. If true, work normally. */
  private boolean allowRackSensitivity = true;

  /** If set, overrides agent placement for all requests to the specified value. */
  private Optional<AgentPlacement> agentPlacementOverride = Optional.empty();

  public boolean isAllowRackSensitivity() {
    return allowRackSensitivity;
  }

  public void setAllowRackSensitivity(boolean allowRackSensitivity) {
    this.allowRackSensitivity = allowRackSensitivity;
  }

  public Optional<AgentPlacement> getAgentPlacementOverride() {
    return agentPlacementOverride;
  }

  public void setAgentPlacementOverride(Optional<AgentPlacement> agentPlacementOverride) {
    this.agentPlacementOverride = agentPlacementOverride;
  }
}
