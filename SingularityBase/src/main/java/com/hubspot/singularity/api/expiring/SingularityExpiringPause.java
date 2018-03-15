package com.hubspot.singularity.api.expiring;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Details about a pause action that will eventually revert")
public class SingularityExpiringPause extends SingularityExpiringRequestActionParent<SingularityPauseRequest> {

  public SingularityExpiringPause(@JsonProperty("requestId") String requestId, @JsonProperty("user") Optional<String> user, @JsonProperty("startMillis") long startMillis,
                                  @JsonProperty("expiringAPIRequestObject") SingularityPauseRequest pauseRequest, @JsonProperty("actionId") String actionId) {
    super(pauseRequest, user, startMillis, actionId, requestId);
  }

  @Override
  public String toString() {
    return "SingularityExpiringPause{} " + super.toString();
  }
}
