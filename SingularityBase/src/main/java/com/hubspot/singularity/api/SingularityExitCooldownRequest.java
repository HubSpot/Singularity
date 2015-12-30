package com.hubspot.singularity.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

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

  public Optional<String> getMessage() {
    return message;
  }

  public Optional<String> getActionId() {
    return actionId;
  }

  public Optional<Boolean> getSkipHealthchecks() {
    return skipHealthchecks;
  }

  @Override
  public String toString() {
    return "SingularityExitCooldownRequest [message=" + message + ", actionId=" + actionId + ", skipHealthchecks=" + skipHealthchecks + "]";
  }

}
