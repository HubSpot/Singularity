package com.hubspot.singularity.expiring;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.api.SingularityPauseRequest;

public class SingularityExpiringPause extends SingularityExpiringParent {

  private final SingularityPauseRequest pauseRequest;

  public SingularityExpiringPause(@JsonProperty("durationMillis") long durationMillis, @JsonProperty("requestId") String requestId, @JsonProperty("user") Optional<String> user,
      @JsonProperty("startMillis") long startMillis, @JsonProperty("pauseRequest") SingularityPauseRequest pauseRequest) {
    super(durationMillis, requestId, user, startMillis);

    this.pauseRequest = pauseRequest;
  }

  public SingularityPauseRequest getPauseRequest() {
    return pauseRequest;
  }

  @Override
  public String toString() {
    return "SingularityExpiringPause [pauseRequest=" + pauseRequest + ", toString()=" + super.toString() + "]";
  }

}
