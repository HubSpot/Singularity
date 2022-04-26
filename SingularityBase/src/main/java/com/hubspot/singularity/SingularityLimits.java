package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Holds the Singularity settings")
public class SingularityLimits {

  private final int maxDecommissioningAgents;

  @JsonCreator
  public SingularityLimits(
    @JsonProperty("maxDecommissioningAgents") int maxDecommissioningAgents
  ) {
    this.maxDecommissioningAgents = maxDecommissioningAgents;
  }

  @Schema(description = "The maximum number of agents that can be decommissioned at once")
  public int getMaxDecommissioningAgents() {
    return maxDecommissioningAgents;
  }
}
