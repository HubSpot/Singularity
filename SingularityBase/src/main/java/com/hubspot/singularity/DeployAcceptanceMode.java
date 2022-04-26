package com.hubspot.singularity;

public enum DeployAcceptanceMode {
  /**
   * Wait for all bound/configured checks to run against the request before proceeding
   */
  CHECKS,
  /**
   * Wait a fixed amount of time between instance groups
   */
  TIMED,
  /**
   * Immediately advance to the next instance group/succeed the deploy once healthy
   */
  NONE,
}
