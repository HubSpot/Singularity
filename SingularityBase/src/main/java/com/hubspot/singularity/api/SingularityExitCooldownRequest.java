package com.hubspot.singularity.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Settings related to how an exit cooldown should behave")
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

  @Schema(description = "A message to show to users about why this action was taken", nullable = true)
  public Optional<String> getMessage() {
    return message;
  }

  @Schema(description = "An id to associate with this action for metadata purposes", nullable = true)
  public Optional<String> getActionId() {
    return actionId;
  }

  @Schema(description = "Instruct new tasks that are scheduled immediately while executing cooldown to skip healthchecks", nullable = true)
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
