package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityDeployProgress {
  private final int targetActiveInstances;
  private final int deployInstanceCountPerStep;
  private final long deployStepWaitTimeSeconds;
  private final boolean stepComplete;
  private final long timestamp;

  public SingularityDeployProgress(@JsonProperty("targetActiveInstances") int targetActiveInstances,
                                   @JsonProperty("deployInstanceCountPerStep") int deployInstanceCountPerStep,
                                   @JsonProperty("deployStepWaitTimeSeconds") long deployStepWaitTimeSeconds,
                                   @JsonProperty("stepComplete") boolean stepComplete,
                                   @JsonProperty("timestamp") long timestamp) {
    this.targetActiveInstances = targetActiveInstances;
    this.deployInstanceCountPerStep = deployInstanceCountPerStep;
    this.deployStepWaitTimeSeconds = deployStepWaitTimeSeconds;
    this.stepComplete = stepComplete;
    this.timestamp = timestamp;
  }

  public int getTargetActiveInstances() {
    return targetActiveInstances;
  }

  public int getDeployInstanceCountPerStep() {
    return deployInstanceCountPerStep;
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
        ", deployInstanceCountPerStep=" + deployInstanceCountPerStep +
        ", deployStepWaitTimeSeconds=" + deployStepWaitTimeSeconds +
        ", stepComplete=" + stepComplete +
        ", timestamp=" + timestamp +
        '}';
  }
}
