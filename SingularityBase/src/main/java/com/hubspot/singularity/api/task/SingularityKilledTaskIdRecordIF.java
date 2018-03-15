package com.hubspot.singularity.api.task;

import java.util.Optional;

import org.immutables.value.Value.Immutable;

import com.hubspot.singularity.annotations.SingularityStyle;
import com.hubspot.singularity.api.request.RequestCleanupType;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema(description = "Describes a task that has been sent a kill signal")
public interface SingularityKilledTaskIdRecordIF {
  @Schema(description = "The unique id of the task")
  SingularityTaskId getTaskId();

  @Schema(description = "the time at which the signal was triggered")
  long getTimestamp();

  @Schema(description = "The original time when the task kill was triggered (in case multiple kills have been issued)")
  long getOriginalTimestamp();

  @Schema(description = "An optional enum cleanup type associated with this task kill", nullable = true)
  Optional<RequestCleanupType> getRequestCleanupType();

  @Schema(description = "An optional enum cleanup type associated with this task kill", nullable = true)
  Optional<TaskCleanupType> getTaskCleanupType();

  @Schema(description = "The number of attempts to kill this task so far")
  int getRetries();
}
