package com.hubspot.singularity.expiring;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.singularity.api.SingularityPauseRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Optional;

@Schema(description = "Details about a pause action that will eventually revert")
public class SingularityExpiringPause
  extends SingularityExpiringRequestActionParent<SingularityPauseRequest> {

  public SingularityExpiringPause(
    @JsonProperty("requestId") String requestId,
    @JsonProperty("user") Optional<String> user,
    @JsonProperty("startMillis") long startMillis,
    @JsonProperty("expiringAPIRequestObject") SingularityPauseRequest pauseRequest,
    @JsonProperty("actionId") String actionId
  ) {
    super(pauseRequest, user, startMillis, actionId, requestId);
  }

  @Override
  public String toString() {
    return "SingularityExpiringPause{} " + super.toString();
  }
}
