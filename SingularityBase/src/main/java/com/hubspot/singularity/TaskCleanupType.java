package com.hubspot.singularity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public enum TaskCleanupType {

  BOUNCING(false, false),
  DECOMMISSION_TIMEOUT(true, true),
  DECOMISSIONING(false, false),
  DEPLOY_CANCELED(true, true),
  DEPLOY_FAILED(true, true),
  DEPLOY_STEP_FINISHED(true, false),
  INCREMENTAL_BOUNCE(false, false),
  INCREMENTAL_DEPLOY_CANCELLED(false, true),
  INCREMENTAL_DEPLOY_FAILED(false, true),
  NEW_DEPLOY_SUCCEEDED(true, false),
  OVERDUE_NEW_TASK(true, true),
  PAUSE(true, true),
  PAUSING(false, false),
  PRIORITY_KILL(true, true),
  REBALANCE_CPU_USAGE(false, false),
  REBALANCE_MEMORY_USAGE(false, false),
  REBALANCE_RACKS(false, false),
  REBALANCE_SLAVE_ATTRIBUTES(false, false),
  REQUEST_DELETING(true, true),
  SCALING_DOWN(true, false),
  TASK_EXCEEDED_TIME_LIMIT(true, true),
  UNHEALTHY_NEW_TASK(true, true),
  USER_REQUESTED(true, true),
  USER_REQUESTED_DESTROY(true, true),
  USER_REQUESTED_TASK_BOUNCE(false, false),
  ;

  private final boolean killLongRunningTaskInstantly;
  private final boolean killNonLongRunningTaskInstantly;

  private TaskCleanupType(boolean killLongRunningTaskInstantly, boolean killNonLongRunningTaskInstantly) {
    this.killLongRunningTaskInstantly = killLongRunningTaskInstantly;
    this.killNonLongRunningTaskInstantly = killNonLongRunningTaskInstantly;
  }

  public boolean shouldKillTaskInstantly(SingularityRequest request) {
    if (request.isLongRunning()) {
      return killLongRunningTaskInstantly;
    } else {
      return killNonLongRunningTaskInstantly;
    }
  }
}
