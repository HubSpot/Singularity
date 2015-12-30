package com.hubspot.singularity.expiring;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.api.SingularityPauseRequest;

public class SingularityExpiringPause extends SingularityExpiringParent<SingularityPauseRequest> {

  public SingularityExpiringPause(@JsonProperty("requestId") String requestId, @JsonProperty("user") Optional<String> user, @JsonProperty("startMillis") long startMillis,
      @JsonProperty("expiringAPIRequestObject") SingularityPauseRequest pauseRequest, @JsonProperty("actionId") String actionId) {
    super(pauseRequest, requestId, user, startMillis, actionId);
  }

  @Override
  public String toString() {
    return "SingularityExpiringPause [toString()=" + super.toString() + "]";
  }

}
