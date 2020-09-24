package com.hubspot.singularity;

/**
 * @deprecated use {@link AgentPlacement}
 */
@Deprecated
public enum SlavePlacement {
  SEPARATE,
  OPTIMISTIC,
  GREEDY,
  SEPARATE_BY_DEPLOY,
  SEPARATE_BY_REQUEST,
  @Deprecated
  SPREAD_ALL_SLAVES,
  SPREAD_ALL_AGENTS
}
