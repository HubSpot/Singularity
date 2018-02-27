package com.hubspot.singularity.api;

import com.google.common.base.Optional;
import com.hubspot.singularity.expiring.SingularityExpiringParent;
import com.hubspot.singularity.expiring.SingularityExpiringSkipHealthchecks;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    title = "Description of a request for a future action",
    subTypes = {
        SingularityMachineChangeRequest.class,
        SingularityExpiringParent.class,
        SingularityScaleRequest.class,
        SingularityBounceRequest.class,
        SingularityExpiringSkipHealthchecks.class
    }
)
public abstract class SingularityExpiringRequestParent {

  private final Optional<Long> durationMillis;
  private final Optional<String> actionId;
  private final Optional<String> message;

  public SingularityExpiringRequestParent(Optional<Long> durationMillis, Optional<String> actionId, Optional<String> message) {
    this.actionId = actionId;
    this.durationMillis = durationMillis;
    this.message = message;
  }

  @Schema(description = "A message to show to users about why this action was taken", nullable = true)
  public Optional<String> getMessage() {
    return message;
  }

  @Schema(description = "An id to associate with this action for metadata purposes", nullable = true)
  public Optional<String> getActionId() {
    return actionId;
  }

  @Schema(description = "The number of milliseconds to wait before reversing the effects of this action (letting it expire)", nullable = true)
  public Optional<Long> getDurationMillis() {
    return durationMillis;
  }

  @Override
  public String toString() {
    return "SingularityExpiringRequestParent{" +
        "durationMillis=" + durationMillis +
        ", actionId=" + actionId +
        ", message=" + message +
        '}';
  }
}
