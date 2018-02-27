package com.hubspot.singularity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.mesos.JavaUtils;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Describes a shell command run against a task")
public class SingularityTaskShellCommandHistory {

  private final SingularityTaskShellCommandRequest shellRequest;
  private final List<SingularityTaskShellCommandUpdate> shellUpdates;

  @JsonCreator
  public SingularityTaskShellCommandHistory(@JsonProperty("shellRequest") SingularityTaskShellCommandRequest shellRequest,
      @JsonProperty("shellUpdates") List<SingularityTaskShellCommandUpdate> shellUpdates) {
    this.shellRequest = shellRequest;
    this.shellUpdates = JavaUtils.nonNullImmutable(shellUpdates);
  }

  @Schema(description = "The request to run the shell command")
  public SingularityTaskShellCommandRequest getShellRequest() {
    return shellRequest;
  }

  @Schema(description = "Updates on the execution of the shell command")
  public List<SingularityTaskShellCommandUpdate> getShellUpdates() {
    return shellUpdates;
  }

  @Override
  public String toString() {
    return "SingularityTaskShellCommandHistory{" +
        "shellRequest=" + shellRequest +
        ", shellUpdates=" + shellUpdates +
        '}';
  }
}
