package com.hubspot.singularity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Optional;

@Schema
public enum RequestCleanupType {
  DELETING(Optional.<TaskCleanupType>empty()),
  PAUSING(Optional.<TaskCleanupType>empty()),
  BOUNCE(Optional.of(TaskCleanupType.BOUNCING)),
  INCREMENTAL_BOUNCE(Optional.of(TaskCleanupType.INCREMENTAL_BOUNCE));

  private final Optional<TaskCleanupType> taskCleanupType;

  private RequestCleanupType(Optional<TaskCleanupType> taskCleanupType) {
    this.taskCleanupType = taskCleanupType;
  }

  public Optional<TaskCleanupType> getTaskCleanupType() {
    return taskCleanupType;
  }
}
