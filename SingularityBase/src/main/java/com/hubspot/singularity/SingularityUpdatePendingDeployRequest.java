package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonProperty;

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

  public String getRequestId() {
    return requestId;
  }

  public String getDeployId() {
    return deployId;
  }

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
