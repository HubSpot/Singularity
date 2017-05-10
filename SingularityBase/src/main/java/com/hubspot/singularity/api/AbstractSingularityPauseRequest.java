package com.hubspot.singularity.api;

import java.util.UUID;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;
import com.hubspot.singularity.SingularityShellCommand;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Immutable
@SingularityStyle
@JsonDeserialize(as = SingularityPauseRequest.class)
public abstract class AbstractSingularityPauseRequest extends SingularityExpiringRequestParent {

  @ApiModelProperty(required=false, value="If set to false, tasks will be allowed to finish instead of killed immediately")
  public abstract Optional<Boolean> getKillTasks();

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

}
