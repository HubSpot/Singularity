package com.hubspot.singularity.api;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Immutable
@SingularityStyle
@JsonDeserialize(as = SingularityExitCooldownRequest.class)
public interface SingularityExitCooldownRequestIF {

  @ApiModelProperty(required=false, value="A message to show to users about why this action was taken")
  Optional<String> getMessage();

  @ApiModelProperty(required=false, value="An id to associate with this action for metadata purposes")
  Optional<String> getActionId();

  @ApiModelProperty(required=false, value="Instruct new tasks that are scheduled immediately while executing cooldown to skip healthchecks")
  Optional<Boolean> getSkipHealthchecks();
}
