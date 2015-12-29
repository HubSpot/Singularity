package com.hubspot.singularity.expiring;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.api.SingularityBounceRequest;

public class SingularityExpiringBounce extends SingularityExpiringParent<SingularityBounceRequest> {

  public SingularityExpiringBounce(@JsonProperty("requestId") String requestId,
      @JsonProperty("user") Optional<String> user, @JsonProperty("startMillis") long startMillis,
      @JsonProperty("expiringAPIRequestObject") SingularityBounceRequest bounceRequest, @JsonProperty("actionId") String actionId) {
    super(bounceRequest, requestId, user, startMillis, actionId);
  }

  @Override
  public String toString() {
    return "SingularityExpiringBounce [toString()=" + super.toString() + "]";
  }

}
