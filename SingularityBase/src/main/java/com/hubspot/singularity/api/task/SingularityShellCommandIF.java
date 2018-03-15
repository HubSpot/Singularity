package com.hubspot.singularity.api.task;

import java.util.List;
import java.util.Optional;

import org.immutables.value.Value.Immutable;

import com.hubspot.singularity.annotations.SingularityStyle;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema(description = "Describes a command to run against an active task")
public interface SingularityShellCommandIF {
  @Schema(required=true, title = "Name of the shell command to run")
  public String getName();

  @Schema(description = "Additional options related to the shell command")
  public Optional<List<String>> getOptions();

  @Schema(description = "User who requested the shell command")
  public Optional<String> getUser();

  @Schema(description = "File name for shell command output")
  public Optional<String> getLogfileName();
}
