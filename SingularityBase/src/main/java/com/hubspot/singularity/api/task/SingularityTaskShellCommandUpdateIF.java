package com.hubspot.singularity.api.task;

import java.util.Optional;

import org.immutables.value.Value.Immutable;

import com.hubspot.singularity.annotations.SingularityStyle;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema(description = "An update to the status of a shell command on a task")
public interface SingularityTaskShellCommandUpdateIF {
  @Schema(description = "The unique shell request id")
  SingularityTaskShellCommandRequestId getShellRequestId();

  @Schema(description = "The time of this update")
  long getTimestamp();

  @Schema(description = "A message associated with this update")
  Optional<String> getMessage();

  @Schema(description = "The file where output was written if the shell command has started running")
  Optional<String> getOutputFilename();

  @Schema(description = "The type of update")
  ShellCommandUpdateType getUpdateType();
}
