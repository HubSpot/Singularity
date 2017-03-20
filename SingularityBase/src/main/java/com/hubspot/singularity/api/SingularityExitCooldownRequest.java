package com.hubspot.singularity.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class SingularityExitCooldownRequest {

  private final Optional<String> message;
  private final Optional<String> actionId;
  private final Optional<Boolean> skipHealthchecks;

  @JsonCreator
  public SingularityExitCooldownRequest(@JsonProperty("message") Optional<String> message,
      @JsonProperty("actionId") Optional<String> actionId, @JsonProperty("skipHealthchecks") Optional<Boolean> skipHealthchecks) {
    this.message = message;
    this.actionId = actionId;
    this.skipHealthchecks = skipHealthchecks;
  }

  @ApiModelProperty(required=false, value="A message to show to users about why this action was taken")
  public Optional<String> getMessage() {
    return message;
  }

  @ApiModelProperty(required=false, value="An id to associate with this action for metadata purposes")
  public Optional<String> getActionId() {
    return actionId;
  }

  @ApiModelProperty(required=false, value="Instruct new tasks that are scheduled immediately while executing cooldown to skip healthchecks")
  public Optional<Boolean> getSkipHealthchecks() {
    return skipHealthchecks;
  }

  @Override
  public String toString() {
    return "SingularityExitCooldownRequest{" +
        "message=" + message +
        ", actionId=" + actionId +
        ", skipHealthchecks=" + skipHealthchecks +
        '}';
  }
}
