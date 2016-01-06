package com.hubspot.singularity;

import com.google.common.base.Optional;

public enum RequestCleanupType {

  DELETING(Optional.<TaskCleanupType> absent()), PAUSING(Optional.<TaskCleanupType> absent()), BOUNCE(Optional.of(TaskCleanupType.BOUNCING)),
  INCREMENTAL_BOUNCE(Optional.of(TaskCleanupType.INCREMENTAL_BOUNCE));

  private final Optional<TaskCleanupType> taskCleanupType;

  private RequestCleanupType(Optional<TaskCleanupType> taskCleanupType) {
    this.taskCleanupType = taskCleanupType;
  }

  public Optional<TaskCleanupType> getTaskCleanupType() {
    return taskCleanupType;
  }

}
