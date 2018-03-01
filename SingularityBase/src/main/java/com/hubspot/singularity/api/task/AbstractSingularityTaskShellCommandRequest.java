package com.hubspot.singularity.api.task;

import java.util.Optional;

import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.primitives.Longs;
import com.hubspot.singularity.annotations.SingularityStyle;
import com.hubspot.singularity.api.common.SingularityFrameworkMessage;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema(description = "A request to run a shell command against a task")
public abstract class AbstractSingularityTaskShellCommandRequest extends SingularityFrameworkMessage implements Comparable<SingularityTaskShellCommandRequest> {

  @Override
  public int compareTo(SingularityTaskShellCommandRequest o) {
    return Longs.compare(o.getTimestamp(), getTimestamp());
  }

  @Schema(description = "The task id to run the command against")
  public abstract SingularityTaskId getTaskId();

  @Schema(description = "The user who initiated this shell command", nullable = true)
  public abstract Optional<String> getUser();

  @Schema(description = "Time the shell command execution was requested")
  public abstract long getTimestamp();

  @Schema(required = true, description = "The shell command to run")
  public abstract SingularityShellCommand getShellCommand();

  @Derived
  @JsonIgnore
  public SingularityTaskShellCommandRequestId getId() {
    return new SingularityTaskShellCommandRequestId(getTaskId(), getShellCommand().getName(), getTimestamp());
  }
}
