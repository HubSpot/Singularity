package com.hubspot.singularity.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.wordnik.swagger.annotations.ApiModelProperty;

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

  @ApiModelProperty(required=false, value="A message to show to users about why this action was taken")
  public Optional<String> getMessage() {
    return message;
  }

  @ApiModelProperty(required=false, value="An id to associate with this action for metadata purposes")
  public Optional<String> getActionId() {
    return actionId;
  }

  @ApiModelProperty(required = false, value = "Should the service associated with the request be removed from the load balancer")
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
