package com.hubspot.singularity.expiring;

import com.google.common.base.Optional;
import com.hubspot.singularity.api.SingularityExpiringRequestParent;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description = "Details about a future action",
    subTypes = {
        SingularityExpiringBounce.class,
        SingularityExpiringPause.class,
        SingularityExpiringScale.class,
        SingularityExpiringSkipHealthchecks.class
    }
)
public abstract class SingularityExpiringRequestActionParent<T extends SingularityExpiringRequestParent> extends SingularityExpiringParent<T> {

  private final String requestId;

  public SingularityExpiringRequestActionParent(T expiringAPIRequestObject, Optional<String> user, long startMillis, String actionId, String requestId) {
    super(expiringAPIRequestObject, user, startMillis, actionId);
    this.requestId = requestId;
  }

  @Schema(description = "The request this future action is in reference to")
  public String getRequestId() {
    return requestId;
  }

  @Override
  public String toString() {
    return "SingularityExpiringRequestActionParent{" +
        "requestId='" + requestId + '\'' +
        "} " + super.toString();
  }
}
