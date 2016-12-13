package com.hubspot.singularity;

public enum TaskCleanupType {

  USER_REQUESTED(true, true), USER_REQUESTED_TASK_BOUNCE(false, false), DECOMISSIONING(false, false), SCALING_DOWN(true, false), BOUNCING(false, false), INCREMENTAL_BOUNCE(false, false),
  DEPLOY_FAILED(true, true), NEW_DEPLOY_SUCCEEDED(true, false), DEPLOY_STEP_FINISHED(true, false), DEPLOY_CANCELED(true, true), TASK_EXCEEDED_TIME_LIMIT(true, true), UNHEALTHY_NEW_TASK(true, true),
  OVERDUE_NEW_TASK(true, true), USER_REQUESTED_DESTROY(true, true), INCREMENTAL_DEPLOY_FAILED(false, true), INCREMENTAL_DEPLOY_CANCELLED(false, true), PRIORITY_KILL(true, true), REBALANCE_RACKS(false, false),
  PAUSING(false, false), PAUSE(true, true), DECOMMISSION_TIMEOUT(true, true);

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
