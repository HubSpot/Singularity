package com.hubspot.singularity.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityShellCommand;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class SingularityKillTaskRequest {

  private final Optional<String> message;
  private final Optional<Boolean> override;
  private final Optional<String> actionId;
  private final Optional<Boolean> waitForReplacementTask;
  private final Optional<SingularityShellCommand> runShellCommandBeforeKill;

  @JsonCreator
  public SingularityKillTaskRequest(@JsonProperty("override") Optional<Boolean> override, @JsonProperty("message") Optional<String> message,
      @JsonProperty("actionId") Optional<String> actionId, @JsonProperty("waitForReplacementTask") Optional<Boolean> waitForReplacementTask,
      @JsonProperty("runShellCommandBeforeKill") Optional<SingularityShellCommand> runShellCommandBeforeKill) {
    this.override = override;
    this.message = message;
    this.actionId = actionId;
    this.waitForReplacementTask = waitForReplacementTask;
    this.runShellCommandBeforeKill = runShellCommandBeforeKill;
  }

  @ApiModelProperty(required=false, value="A message to show to users about why this action was taken")
  public Optional<String> getMessage() {
    return message;
  }

  @ApiModelProperty(required=false, value="An id to associate with this action for metadata purposes")
  public Optional<String> getActionId() {
    return actionId;
  }

  @ApiModelProperty(required=false, value="If set to true, instructs the executor to attempt to immediately kill the task, rather than waiting gracefully")
  public Optional<Boolean> getOverride() {
    return override;
  }

  @ApiModelProperty(required=false, value="If set to true, treats this task kill as a bounce - launching another task and waiting for it to become healthy")
  public Optional<Boolean> getWaitForReplacementTask() {
    return waitForReplacementTask;
  }

  @ApiModelProperty(required=false, value="Attempt to run this shell command on each task before it is shut down")
  public Optional<SingularityShellCommand> getRunShellCommandBeforeKill() {
    return runShellCommandBeforeKill;
  }

  @Override
  public String toString() {
    return "SingularityKillTaskRequest [message=" + message + ", override=" + override + ", actionId=" + actionId + ", waitForReplacementTask=" + waitForReplacementTask + ", runShellCommandBeforeKill=" + runShellCommandBeforeKill
      + "]";
  }

}
