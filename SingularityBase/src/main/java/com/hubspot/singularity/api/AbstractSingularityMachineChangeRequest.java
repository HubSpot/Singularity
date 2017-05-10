package com.hubspot.singularity.api;

import java.util.UUID;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;
import com.hubspot.singularity.MachineState;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Immutable
@SingularityStyle
@JsonDeserialize(as = SingularityMachineChangeRequest.class)
public abstract class AbstractSingularityMachineChangeRequest extends SingularityExpiringRequestParent {

  @ApiModelProperty(required=false, value="If a durationMillis is specified, return to this state when time has elapsed")
  public abstract Optional<MachineState> getRevertToState();

  @ApiModelProperty(required=false, value="If a machine has not successfully decommissioned in durationMillis, kill the remaining tasks on the machine")
  public abstract boolean isKillTasksOnDecommissionTimeout();

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
