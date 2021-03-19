package com.hubspot.singularity;

public enum DeployAcceptanceMode {
  /**
   * Wait for an api call to advance the deploy forward to the next instance group
   */
  API,
  /**
   * Wait a fixed amount of time between instance groups
   */
  TIMED,
  /**
   * Immediately advance to the next instance group/succeed the deploy once healthy
   */
  NONE
}
