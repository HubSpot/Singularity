package com.hubspot.singularity.expiring;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.singularity.api.SingularityPriorityRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Optional;

@Schema(description = "Details about a scale action that will eventually revert")
public class SingularityExpiringPriority
  extends SingularityExpiringRequestActionParent<SingularityPriorityRequest> {

  private final Optional<Double> revertToPriority;

  public SingularityExpiringPriority(
    @JsonProperty("requestId") String requestId,
    @JsonProperty("user") Optional<String> user,
    @JsonProperty("startMillis") long startMillis,
    @JsonProperty("expiringAPIRequestObject") SingularityPriorityRequest request,
    @JsonProperty("revertToPriority") Optional<Double> revertToPriority,
    @JsonProperty("actionId") String actionId
  ) {
    super(request, user, startMillis, actionId, requestId);
    this.revertToPriority = revertToPriority;
  }

  @Schema(
    description = "The scheduling priority to update to when time has elapsed",
    nullable = true
  )
  public Optional<Double> getRevertToPriority() {
    return revertToPriority;
  }

  @Override
  public String toString() {
    return "SingularityExpiringPriority{" + "revertToPriority=" + revertToPriority + '}';
  }
}
