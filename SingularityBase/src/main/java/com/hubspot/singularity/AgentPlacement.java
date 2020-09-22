package com.hubspot.singularity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
  description = "Determines what constraints are applied when selecting an offer for a task"
)
public enum AgentPlacement {
  SEPARATE,
  OPTIMISTIC,
  GREEDY,
  SEPARATE_BY_DEPLOY,
  SEPARATE_BY_REQUEST,
  @Deprecated
  SPREAD_ALL_SLAVES,
  SPREAD_ALL_AGENTS
}
