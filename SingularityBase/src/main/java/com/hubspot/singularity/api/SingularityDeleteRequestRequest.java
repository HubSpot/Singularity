package com.hubspot.singularity.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Settings related to deleting a request")
public class SingularityDeleteRequestRequest {

  private final Optional<String> message;
  private final Optional<String> actionId;
  private final Optional<Boolean> deleteFromLoadBalancer;

  @JsonCreator
  public SingularityDeleteRequestRequest(@JsonProperty("message") Optional<String> message,
                                         @JsonProperty("actionId") Optional<String> actionId,
                                         @JsonProperty("deleteFromLoadBalancer") Optional<Boolean> deleteFromLoadBalancer) {
    this.message = message;
    this.actionId = actionId;
    this.deleteFromLoadBalancer = deleteFromLoadBalancer;
  }

  @Schema(description = "A message to show to users about why this action was taken", nullable = true)
  public Optional<String> getMessage() {
    return message;
  }

  @Schema(description = "An id to associate with this action for metadata purposes", nullable = true)
  public Optional<String> getActionId() {
    return actionId;
  }

  @Schema(description = "Should the service associated with the request be removed from the load balancer", nullable = true)
  public Optional<Boolean> getDeleteFromLoadBalancer() {
    return deleteFromLoadBalancer;
  }

  @Override
  public String toString() {
    return "SingularityDeleteRequestRequest{" +
        "message=" + message +
        ", actionId=" + actionId +
        ", deleteFromLoadBalancer=" + deleteFromLoadBalancer +
        '}';
  }
}
