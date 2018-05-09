package com.hubspot.singularity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public enum DeployState {
  SUCCEEDED(TaskCleanupType.DEPLOY_STEP_FINISHED), FAILED_INTERNAL_STATE(TaskCleanupType.DEPLOY_FAILED), CANCELING(null), WAITING(null), OVERDUE(TaskCleanupType.DEPLOY_FAILED), FAILED(TaskCleanupType.DEPLOY_FAILED), CANCELED(TaskCleanupType.DEPLOY_CANCELED);

  private final TaskCleanupType cleanupType;
  private final boolean isDeployFinished;

  private DeployState(TaskCleanupType cleanupType) {
    this.cleanupType = cleanupType;
    this.isDeployFinished = cleanupType != null;
  }

  @Schema(description = "True if the deploy is in a terminal state")
  public boolean isDeployFinished() {
    return isDeployFinished;
  }

  @Schema(description = "The type of task cleanup created for tasks related to this deploy (if failed), or an older deploy (if succeeded)")
  public TaskCleanupType getCleanupType() {
    return cleanupType;
  }

}
