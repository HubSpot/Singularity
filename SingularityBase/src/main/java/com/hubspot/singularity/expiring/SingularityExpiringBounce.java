package com.hubspot.singularity.expiring;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.api.SingularityBounceRequest;

public class SingularityExpiringBounce extends SingularityExpiringParent<SingularityBounceRequest> {

  private final String deployId;

  public SingularityExpiringBounce(@JsonProperty("requestId") String requestId, @JsonProperty("deployId") String deployId,
      @JsonProperty("user") Optional<String> user, @JsonProperty("startMillis") long startMillis,
      @JsonProperty("expiringAPIRequestObject") SingularityBounceRequest bounceRequest, @JsonProperty("actionId") String actionId) {
    super(bounceRequest, requestId, user, startMillis, actionId);

    this.deployId = deployId;
  }

  public String getDeployId() {
    return deployId;
  }

  public static Optional<SingularityExpiringBounce> withDefaultExpiringMillis(
      Optional<SingularityExpiringBounce> maybeExpiringBounce,
      Long durationMillis) {
    if (!maybeExpiringBounce.isPresent()) {
      return maybeExpiringBounce;
    }
    final SingularityExpiringBounce bounce = maybeExpiringBounce.get();
    if (bounce.getExpiringAPIRequestObject().getDurationMillis().isPresent()) {
      return maybeExpiringBounce;
    }
    return Optional.of(
        new SingularityExpiringBounce(
            bounce.getRequestId(),
            bounce.getDeployId(),
            bounce.getUser(),
            bounce.getStartMillis(),
            new SingularityBounceRequest(
                bounce.getExpiringAPIRequestObject().getIncremental(),
                bounce.getExpiringAPIRequestObject().getSkipHealthchecks(),
                Optional.of(durationMillis),
                bounce.getExpiringAPIRequestObject().getActionId(),
                bounce.getExpiringAPIRequestObject().getMessage()
            ),
            bounce.getActionId()
        )
    );
  }

  @Override
  public String toString() {
    return "SingularityExpiringBounce [toString()=" + super.toString() + "]";
  }

}
