package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class SingularityUpdatePendingDeployRequest {
  private final String requestId;
  private final String deployId;
  private final int targetActiveInstances;

  public SingularityUpdatePendingDeployRequest(@JsonProperty("requestId") String requestId,
    @JsonProperty("deployId") String deployId,
    @JsonProperty("targetActiveInstances") int targetActiveInstances) {
    this.requestId = requestId;
    this.deployId = deployId;
    this.targetActiveInstances = targetActiveInstances;
  }

  @ApiModelProperty(value="Request id", required=true)
  public String getRequestId() {
    return requestId;
  }

  @ApiModelProperty(value="Deploy id", required=true)
  public String getDeployId() {
    return deployId;
  }

  @ApiModelProperty(value="Updated target instance count for the active deploy", required=true)
  public int getTargetActiveInstances() {
    return targetActiveInstances;
  }

  @Override
  public String toString() {
    return "SingularityUpdatePendingDeployRequest{" +
      "requestId=" + requestId +
      ", deployId=" + deployId +
      ", targetActiveInstances=" + targetActiveInstances +
      '}';
  }
}
