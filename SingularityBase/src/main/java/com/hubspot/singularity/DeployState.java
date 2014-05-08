package com.hubspot.singularity;

import com.hubspot.singularity.SingularityTaskCleanup.TaskCleanupType;

public enum DeployState {
  SUCCEEDED(TaskCleanupType.NEW_DEPLOY_SUCCEEDED), FAILED_INTERNAL_STATE(TaskCleanupType.DEPLOY_FAILED), CANCELING(null), WAITING(null), OVERDUE(TaskCleanupType.DEPLOY_FAILED), FAILED(TaskCleanupType.DEPLOY_FAILED), CANCELED(TaskCleanupType.DEPLOY_CANCELED);

  private final TaskCleanupType cleanupType;
  private final boolean isDeployFinished;

  private DeployState(TaskCleanupType cleanupType) {
    this.cleanupType = cleanupType;
    this.isDeployFinished = cleanupType != null;
  }
  
  public boolean isDeployFinished() {
    return isDeployFinished;
  }

  public TaskCleanupType getCleanupType() {
    return cleanupType;
  }
  
}
