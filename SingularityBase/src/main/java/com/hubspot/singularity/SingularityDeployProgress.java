package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityDeployProgress {
  private final int targetActiveInstances;
  private final int deployInstanceCountPerStep;
  private final long deployStepWaitTimeMs;
  private final boolean stepComplete;
  private final boolean autoAdvanceDeploySteps;
  private final long timestamp;

  @JsonCreator
  public SingularityDeployProgress(@JsonProperty("targetActiveInstances") int targetActiveInstances, @JsonProperty("deployInstanceCountPerStep") int deployInstanceCountPerStep,
    @JsonProperty("deployStepWaitTimeMs") long deployStepWaitTimeMs, @JsonProperty("stepComplete") boolean stepComplete,
    @JsonProperty("autoAdvanceDeploySteps") boolean autoAdvanceDeploySteps, @JsonProperty("timestamp") long timestamp) {
    this.targetActiveInstances = targetActiveInstances;
    this.deployInstanceCountPerStep = deployInstanceCountPerStep;
    this.deployStepWaitTimeMs = deployStepWaitTimeMs;
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

  public long getDeployStepWaitTimeMs() {
    return deployStepWaitTimeMs;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public SingularityDeployProgress withNewInstances(int instances) {
    return new SingularityDeployProgress(instances, deployInstanceCountPerStep, deployStepWaitTimeMs, false, autoAdvanceDeploySteps, System.currentTimeMillis());
  }

  public SingularityDeployProgress withCompletedStep() {
    return new SingularityDeployProgress(targetActiveInstances, deployInstanceCountPerStep, deployStepWaitTimeMs, true, autoAdvanceDeploySteps, System.currentTimeMillis());
  }

  @Override
  public String toString() {
    return "SingularityIncrementalDeployProgress{" +
      "targetActiveInstances=" + targetActiveInstances +
      ", deployInstanceCountPerStep=" + deployInstanceCountPerStep +
      ", deployStepWaitTimeMs=" + deployStepWaitTimeMs +
      ", stepComplete=" + stepComplete +
      ", autoAdvanceDeploySteps=" + autoAdvanceDeploySteps +
      ", timestamp=" + timestamp +
      '}';
  }
}
