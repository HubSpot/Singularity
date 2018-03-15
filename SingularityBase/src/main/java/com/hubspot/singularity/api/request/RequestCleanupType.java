package com.hubspot.singularity.api.request;

import java.util.Optional;

import com.hubspot.singularity.api.task.TaskCleanupType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public enum RequestCleanupType {

  DELETING(Optional.empty()), PAUSING(Optional.empty()), BOUNCE(Optional.of(TaskCleanupType.BOUNCING)),
  INCREMENTAL_BOUNCE(Optional.of(TaskCleanupType.INCREMENTAL_BOUNCE));

  private final Optional<TaskCleanupType> taskCleanupType;

  private RequestCleanupType(Optional<TaskCleanupType> taskCleanupType) {
    this.taskCleanupType = taskCleanupType;
  }

  public Optional<TaskCleanupType> getTaskCleanupType() {
    return taskCleanupType;
  }

}
