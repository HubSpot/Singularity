package com.hubspot.singularity.api;

import com.google.common.base.Optional;
import com.wordnik.swagger.annotations.ApiModelProperty;

public abstract class SingularityExpiringRequestParent {

  private final Optional<Long> durationMillis;
  private final Optional<String> actionId;
  private final Optional<String> message;

  public SingularityExpiringRequestParent(Optional<Long> durationMillis, Optional<String> actionId, Optional<String> message) {
    this.actionId = actionId;
    this.durationMillis = durationMillis;
    this.message = message;
  }

  @ApiModelProperty(required=false, value="A message to show to users about why this action was taken")
  public Optional<String> getMessage() {
    return message;
  }

  @ApiModelProperty(required=false, value="An id to associate with this action for metadata purposes")
  public Optional<String> getActionId() {
    return actionId;
  }

  @ApiModelProperty(required=false, value="The number of milliseconds to wait before reversing the effects of this action (letting it expire)")
  public Optional<Long> getDurationMillis() {
    return durationMillis;
  }

  @Override
  public String toString() {
    return "SingularityExpiringRequestParent{" +
        "durationMillis=" + durationMillis +
        ", actionId=" + actionId +
        ", message=" + message +
        '}';
  }
}
