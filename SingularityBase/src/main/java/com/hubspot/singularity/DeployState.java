package com.hubspot.singularity;

import com.hubspot.singularity.SingularityTaskCleanup.TaskCleanupType;

public enum DeployState {
  SUCCEEDED(TaskCleanupType.NEW_DEPLOY_SUCCEEDED), FAILED_INTERNAL_STATE(TaskCleanupType.DEPLOY_FAILED), WAITING(null), OVERDUE(TaskCleanupType.DEPLOY_FAILED), FAILED(TaskCleanupType.DEPLOY_FAILED), CANCELED(TaskCleanupType.DEPLOY_CANCELED);

  private final TaskCleanupType cleanupType;

  private DeployState(TaskCleanupType cleanupType) {
    this.cleanupType = cleanupType;
  }

  public TaskCleanupType getCleanupType() {
    return cleanupType;
  }
  
}
