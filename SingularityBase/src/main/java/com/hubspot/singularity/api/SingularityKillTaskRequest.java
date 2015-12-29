package com.hubspot.singularity.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityKillTaskRequest {

  private final Optional<String> message;
  private final Optional<Boolean> override;
  private final Optional<String> actionId;

  @JsonCreator
  public SingularityKillTaskRequest(@JsonProperty("override") Optional<Boolean> override, @JsonProperty("message") Optional<String> message,
      @JsonProperty("actionId") Optional<String> actionId) {
    this.override = override;
    this.message = message;
    this.actionId = actionId;
  }

  public Optional<String> getMessage() {
    return message;
  }

  public Optional<Boolean> getOverride() {
    return override;
  }

  public Optional<String> getActionId() {
    return actionId;
  }

  @Override
  public String toString() {
    return "SingularityKillTaskRequest [message=" + message + ", override=" + override + ", actionId=" + actionId + "]";
  }

}
