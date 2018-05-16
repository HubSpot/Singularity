package com.hubspot.singularity.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityShellCommand;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Settings for how a pause should behave")
public class SingularityPauseRequest extends SingularityExpiringRequestParent {

  private final Optional<Boolean> killTasks;
  private final Optional<SingularityShellCommand> runShellCommandBeforeKill;


  @JsonCreator
  public SingularityPauseRequest(@JsonProperty("killTasks") Optional<Boolean> killTasks,@JsonProperty("durationMillis") Optional<Long> durationMillis,
      @JsonProperty("actionId") Optional<String> actionId, @JsonProperty("message") Optional<String> message, @JsonProperty("runShellCommandBeforeKill") Optional<SingularityShellCommand> runShellCommandBeforeKill) {
    super(durationMillis, actionId, message);

    this.killTasks = killTasks;
    this.runShellCommandBeforeKill = runShellCommandBeforeKill;
  }

  @Schema(description = "If set to false, tasks will be allowed to finish instead of killed immediately", nullable = true)
  public Optional<Boolean> getKillTasks() {
    return killTasks;
  }

  @Schema(description = "Attempt to run this shell command on each task before it is shut down", nullable = true)
  public Optional<SingularityShellCommand> getRunShellCommandBeforeKill() {
    return runShellCommandBeforeKill;
  }

  @Override
  public String toString() {
    return "SingularityPauseRequest{" +
        "killTasks=" + killTasks +
        ", runShellCommandBeforeKill=" + runShellCommandBeforeKill +
        "} " + super.toString();
  }
}
