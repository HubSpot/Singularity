package com.hubspot.singularity.api.task;

import java.util.Optional;

import org.immutables.value.Value.Immutable;

import com.hubspot.singularity.annotations.SingularityStyle;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema(description = "Overrides related to how a task kill is performed")
public interface SingularityKillTaskRequestIF {
  @Schema(nullable = true, description = "If set to true, instructs the executor to attempt to immediately kill the task, rather than waiting gracefully")
  Optional<Boolean> getOverride();

  @Schema(nullable = true, description = "A message to show to users about why this action was taken")
  Optional<String> getMessage();

  @Schema(nullable = true, description = "An id to associate with this action for metadata purposes")
  Optional<String> getActionId();

  @Schema(nullable = true, description = "If set to true, treats this task kill as a bounce - launching another task and waiting for it to become healthy")
  Optional<Boolean> getWaitForReplacementTask();

  @Schema(nullable = true, description = "Attempt to run this shell command on each task before it is shut down")
  Optional<SingularityShellCommand> getRunShellCommandBeforeKill();
}
