package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityDeployProgress {
  private final int targetActiveInstances;
  private final int deployRate;
  private final long deployStepWaitTimeSeconds;
  private final boolean stepComplete;
  private final long timestamp;

  public SingularityDeployProgress(@JsonProperty("targetActiveInstances") int targetActiveInstances,
                                   @JsonProperty("deployRate") int deployRate,
                                   @JsonProperty("deployStepWaitTimeSeconds") long deployStepWaitTimeSeconds,
                                   @JsonProperty("stepComplete") boolean stepComplete,
                                   @JsonProperty("timestamp") long timestamp) {
    this.targetActiveInstances = targetActiveInstances;
    this.deployRate = deployRate;
    this.deployStepWaitTimeSeconds = deployStepWaitTimeSeconds;
    this.stepComplete = stepComplete;
    this.timestamp = timestamp;
  }

  public int getTargetActiveInstances() {
    return targetActiveInstances;
  }

  public int getDeployRate() {
    return deployRate;
  }

  public boolean isStepComplete() {
    return stepComplete;
  }

  public long getDeployStepWaitTimeSeconds() {
    return deployStepWaitTimeSeconds;
  }

  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public String toString() {
    return "SingularityIncrementalDeployProgress{" +
        "targetActiveInstances=" + targetActiveInstances +
        ", deployRate=" + deployRate +
        ", deployStepWaitTimeSeconds=" + deployStepWaitTimeSeconds +
        ", stepComplete=" + stepComplete +
        ", timestamp=" + timestamp +
        '}';
  }
}
