package com.hubspot.singularity.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityDeleteRequestRequest {

  private final Optional<String> message;
  private final Optional<String> actionId;

  @JsonCreator
  public SingularityDeleteRequestRequest(@JsonProperty("message") Optional<String> message,
      @JsonProperty("actionId") Optional<String> actionId) {
    this.message = message;
    this.actionId = actionId;
  }

  public Optional<String> getMessage() {
    return message;
  }

  public Optional<String> getActionId() {
    return actionId;
  }

  @Override
  public String toString() {
    return "SingularityDeleteRequestRequest [message=" + message + ", actionId=" + actionId + "]";
  }

}
