package com.hubspot.singularity;

import java.util.Optional;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public enum RequestCleanupType {

  DELETING(Optional.<TaskCleanupType>empty()), PAUSING(Optional.<TaskCleanupType>empty()), BOUNCE(Optional.of(TaskCleanupType.BOUNCING)),
  INCREMENTAL_BOUNCE(Optional.of(TaskCleanupType.INCREMENTAL_BOUNCE));

  private final Optional<TaskCleanupType> taskCleanupType;

  private RequestCleanupType(Optional<TaskCleanupType> taskCleanupType) {
    this.taskCleanupType = taskCleanupType;
  }

  public Optional<TaskCleanupType> getTaskCleanupType() {
    return taskCleanupType;
  }

}
