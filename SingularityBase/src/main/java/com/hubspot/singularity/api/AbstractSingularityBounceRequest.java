package com.hubspot.singularity.api;

import java.util.UUID;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;
import com.hubspot.singularity.SingularityShellCommand;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Immutable
@SingularityStyle
public abstract class AbstractSingularityBounceRequest extends SingularityExpiringRequestParent {

  public static SingularityBounceRequest defaultRequest() {
    return SingularityBounceRequest.builder().build();
  }

  @Default
  @ApiModelProperty(required=false, value="If present and set to true, old tasks will be killed as soon as replacement tasks are available, instead of waiting for all replacement tasks to be healthy")
  public boolean isIncremental() {
    return true;
  }

  @Default
  @ApiModelProperty(required=false, value="Instruct replacement tasks for this bounce only to skip healthchecks")
  public boolean isSkipHealthchecks() {
    return false;
  }

  @ApiModelProperty(required=false, value="Attempt to run this shell command on each task before it is shut down")
  public abstract Optional<SingularityShellCommand> getRunShellCommandBeforeKill();

  ///
  @ApiModelProperty(required=false, value="A message to show to users about why this action was taken")
  public abstract Optional<String> getMessage();

  @Default
  @ApiModelProperty(required=false, value="An id to associate with this action for metadata purposes")
  public String getActionId() {
    return UUID.randomUUID().toString();
  }

  @ApiModelProperty(required=false, value="The number of milliseconds to wait before reversing the effects of this action (letting it expire)")
  public abstract Optional<Long> getDurationMillis();


  public static SingularityBounceRequest of(Optional<Boolean> incremental,
                                            Optional<Boolean> skipHealthchecks,
                                            Optional<Long> durationMillis,
                                            Optional<String> actionId,
                                            Optional<String> message,
                                            Optional<SingularityShellCommand> runShellCommandBeforeKill) {
    return SingularityBounceRequest.builder()
        .setIncremental(incremental.or(true))
        .setSkipHealthchecks(skipHealthchecks.or(false))
        .setDurationMillis(durationMillis)
        .setActionId(actionId.or(UUID.randomUUID().toString()))
        .setMessage(message)
        .setRunShellCommandBeforeKill(runShellCommandBeforeKill)
        .build();
  }
}
