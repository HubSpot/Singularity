package com.hubspot.singularity;

public class SingularityLimits {
  private final int maxDecommissioningAgents;

  public SingularityLimits(int maxDecommissioningAgents) {
    this.maxDecommissioningAgents = maxDecommissioningAgents;
  }

  public int getMaxDecommissioningAgents() {
    return maxDecommissioningAgents;
  }
}
