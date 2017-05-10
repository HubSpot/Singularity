package com.hubspot.singularity.api;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;
import com.hubspot.singularity.SingularityAction;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Immutable
@SingularityStyle
@JsonDeserialize(as = SingularityDisabledActionRequest.class)
public interface SingularityDisabledActionRequestIF {
  @ApiModelProperty(required=true, value="The type of action to disable")
  SingularityAction getType();

  @ApiModelProperty(required=false, value="An optional message/reason for disabling the action specified")
  Optional<String> getMessage();
}
