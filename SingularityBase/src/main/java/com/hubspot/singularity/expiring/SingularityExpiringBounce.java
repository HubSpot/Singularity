package com.hubspot.singularity.expiring;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.api.SingularityBounceRequest;

public class SingularityExpiringBounce extends SingularityExpiringParent {

  private final SingularityBounceRequest bounceRequest;

  public SingularityExpiringBounce(@JsonProperty("durationMillis") long durationMillis, @JsonProperty("requestId") String requestId, @JsonProperty("user") Optional<String> user,
      @JsonProperty("startMillis") long startMillis, @JsonProperty("bounceRequest") SingularityBounceRequest bounceRequest) {
    super(durationMillis, requestId, user, startMillis);

    this.bounceRequest = bounceRequest;
  }

  public SingularityBounceRequest getBounceRequest() {
    return bounceRequest;
  }

  @Override
  public String toString() {
    return "SingularityExpiringBounce [bounceRequest=" + bounceRequest + ", toString()=" + super.toString() + "]";
  }

}
