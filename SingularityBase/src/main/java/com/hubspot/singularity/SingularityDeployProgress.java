package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityDeployProgress {
  private final int targetActiveInstances;
  private final int deployInstanceCountPerStep;
  private final long deployStepWaitTimeSeconds;
  private final boolean stepComplete;
  private final boolean  autoAdvanceDeploySteps;
  private final long timestamp;

  public SingularityDeployProgress(@JsonProperty("targetActiveInstances") int targetActiveInstances,
                                   @JsonProperty("deployInstanceCountPerStep") int deployInstanceCountPerStep,
                                   @JsonProperty("deployStepWaitTimeSeconds") long deployStepWaitTimeSeconds,
                                   @JsonProperty("stepComplete") boolean stepComplete,
                                   @JsonProperty("autoAdvance") boolean autoAdvanceDeploySteps,
                                   @JsonProperty("timestamp") long timestamp) {
    this.targetActiveInstances = targetActiveInstances;
    this.deployInstanceCountPerStep = deployInstanceCountPerStep;
    this.deployStepWaitTimeSeconds = deployStepWaitTimeSeconds;
    this.stepComplete = stepComplete;
    this.autoAdvanceDeploySteps = autoAdvanceDeploySteps;
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

  public boolean isAutoAdvanceDeploySteps() {
    return autoAdvanceDeploySteps;
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
        ", autoAdvanceDeploySteps=" +  autoAdvanceDeploySteps +
        ", timestamp=" + timestamp +
        '}';
  }
}
