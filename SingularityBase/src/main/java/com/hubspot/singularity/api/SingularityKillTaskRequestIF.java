package com.hubspot.singularity.api;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;
import com.hubspot.singularity.SingularityShellCommand;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Immutable
@SingularityStyle
@JsonDeserialize(as = SingularityKillTaskRequest.class)
public interface SingularityKillTaskRequestIF {
  @ApiModelProperty(required=false, value="If set to true, instructs the executor to attempt to immediately kill the task, rather than waiting gracefully")
  Optional<Boolean> getOverride();

  @ApiModelProperty(required=false, value="A message to show to users about why this action was taken")
  Optional<String> getMessage();

  @ApiModelProperty(required=false, value="An id to associate with this action for metadata purposes")
  Optional<String> getActionId();

  @ApiModelProperty(required=false, value="If set to true, treats this task kill as a bounce - launching another task and waiting for it to become healthy")
  Optional<Boolean> getWaitForReplacementTask();

  @ApiModelProperty(required=false, value="Attempt to run this shell command on each task before it is shut down")
  Optional<SingularityShellCommand> getRunShellCommandBeforeKill();
}
