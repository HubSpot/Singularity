package com.hubspot.singularity.expiring;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.api.SingularityBounceRequest;

public class SingularityExpiringBounce extends SingularityExpiringRequestActionParent<SingularityBounceRequest> {

  private final String deployId;

  public SingularityExpiringBounce(@JsonProperty("requestId") String requestId, @JsonProperty("deployId") String deployId,
      @JsonProperty("user") Optional<String> user, @JsonProperty("startMillis") long startMillis,
      @JsonProperty("expiringAPIRequestObject") SingularityBounceRequest bounceRequest, @JsonProperty("actionId") String actionId) {
    super(bounceRequest, user, startMillis, actionId, requestId);

    this.deployId = deployId;
  }

  public String getDeployId() {
    return deployId;
  }

  @Override
  public String toString() {
    return "SingularityExpiringBounce [toString()=" + super.toString() + "]";
  }

}
