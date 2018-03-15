package com.hubspot.singularity.api.task;

import java.util.Optional;

import org.immutables.value.Value.Immutable;

import com.hubspot.singularity.annotations.SingularityStyle;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema(description = "Describes the cleanup or shutdown of a singularity task")
public interface SingularityTaskCleanupIF {
  @Schema(description = "The user who triggered this cleanup", nullable = true)
  Optional<String> getUser();

  @Schema(description = "The enum reason for this cleanup")
  TaskCleanupType getCleanupType();

  @Schema(description = "The time this cleanup was created")
  long getTimestamp();

  @Schema(description = "The unique id of the task being cleaned up")
  SingularityTaskId getTaskId();

  @Schema(description = "An optional message describing the reason this task was cleaned", nullable = true)
  Optional<String> getMessage();


  @Schema(description = "An optional unique id associted with the cleanup of this task", nullable = true)
  Optional<String> getActionId();
  @Schema(description = "An optional command to run before shutting down the task")
  Optional<SingularityTaskShellCommandRequestId> getRunBeforeKillId();

  @Schema(
      title = "If a request is being deleted and this is the final task, trigger deletion of the request from the load balancer",
      defaultValue = "true"
  )
  Optional<Boolean> getRemoveFromLoadBalancer();
}
