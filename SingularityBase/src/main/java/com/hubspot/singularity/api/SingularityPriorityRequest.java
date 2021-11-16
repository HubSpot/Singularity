package com.hubspot.singularity.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Optional;

@Schema(description = "Describes the new priority for a request")
public class SingularityPriorityRequest extends SingularityExpiringRequestParent {
  private final Optional<Double> priority;

  public SingularityPriorityRequest(
    @JsonProperty("durationMillis") Optional<Long> durationMillis,
    @JsonProperty("actionId") Optional<String> actionId,
    @JsonProperty("message") Optional<String> message,
    @JsonProperty("priority") Optional<Double> priority,
    @JsonProperty("skipEmailNotification") Optional<Boolean> skipEmailNotification
  ) {
    super(durationMillis, actionId, message);
    this.priority = priority;
  }

  public Optional<Double> getPriority() {
    return priority;
  }
}
