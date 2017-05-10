package com.hubspot.singularity.api;

import com.google.common.base.Optional;
import com.wordnik.swagger.annotations.ApiModelProperty;

public abstract class SingularityExpiringRequestParent {

  @ApiModelProperty(required=false, value="A message to show to users about why this action was taken")
  public abstract Optional<String> getMessage();

  @ApiModelProperty(required=false, value="An id to associate with this action for metadata purposes")
  public abstract String getActionId();

  @ApiModelProperty(required=false, value="The number of milliseconds to wait before reversing the effects of this action (letting it expire)")
  public abstract Optional<Long> getDurationMillis();

}
