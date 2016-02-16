package com.hubspot.singularity;

public enum DeployState {
  SUCCEEDED(TaskCleanupType.DEPLOY_STEP_FINISHED), FAILED_INTERNAL_STATE(TaskCleanupType.DEPLOY_FAILED), CANCELING(null), WAITING(null), OVERDUE(TaskCleanupType.DEPLOY_FAILED), FAILED(TaskCleanupType.DEPLOY_FAILED), CANCELED(TaskCleanupType.DEPLOY_CANCELED);

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
