package com.hubspot.singularity.api;

import org.immutables.value.Value.Immutable;

import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Immutable
@SingularityStyle
public interface SingularityUnpauseRequestIF {

  @ApiModelProperty(required=false, value="A message to show to users about why this action was taken")
  Optional<String> getMessage();

  @ApiModelProperty(required=false, value="An id to associate with this action for metadata purposes")
  Optional<String> getActionId();

  @ApiModelProperty(required=false, value="If set to true, instructs new tasks that are scheduled immediately while unpausing to skip healthchecks")
  Optional<Boolean> getSkipHealthchecks();
}
