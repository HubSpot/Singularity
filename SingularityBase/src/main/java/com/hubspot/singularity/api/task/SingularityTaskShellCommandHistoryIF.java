package com.hubspot.singularity.api.task;

import java.util.List;

import org.immutables.value.Value.Immutable;

import com.hubspot.singularity.annotations.SingularityStyle;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema(description = "Describes a shell command run against a task")
public interface SingularityTaskShellCommandHistoryIF {
  @Schema(description = "The request to run the shell command")
  SingularityTaskShellCommandRequest getShellRequest();

  @Schema(description = "Updates on the execution of the shell command")
  List<SingularityTaskShellCommandUpdate> getShellUpdates();
}
