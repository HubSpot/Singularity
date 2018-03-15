package com.hubspot.singularity.api.expiring;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Details about a bounce that will eventually expire/give up")
public class SingularityExpiringBounce extends SingularityExpiringRequestActionParent<SingularityBounceRequest> {

  private final String deployId;

  public SingularityExpiringBounce(@JsonProperty("requestId") String requestId, @JsonProperty("deployId") String deployId,
                                   @JsonProperty("user") Optional<String> user, @JsonProperty("startMillis") long startMillis,
                                   @JsonProperty("expiringAPIRequestObject") SingularityBounceRequest bounceRequest, @JsonProperty("actionId") String actionId) {
    super(bounceRequest, user, startMillis, actionId, requestId);

    this.deployId = deployId;
  }

  @Schema(description = "The deploy associated with this expiring bounce")
  public String getDeployId() {
    return deployId;
  }

  @Override
  public String toString() {
    return "SingularityExpiringBounce{" +
        "deployId='" + deployId + '\'' +
        "} " + super.toString();
  }
}
