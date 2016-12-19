package com.hubspot.singularity;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityDeployProgress {
  private final int targetActiveInstances;
  private final int currentActiveInstances;
  private final int deployInstanceCountPerStep;
  private final long deployStepWaitTimeMs;
  private final boolean stepComplete;
  private final boolean autoAdvanceDeploySteps;
  private final Set<SingularityTaskId> failedDeployTasks;
  private final long timestamp;
  @JsonCreator
  public SingularityDeployProgress(@JsonProperty("targetActiveInstances") int targetActiveInstances,
                                   @JsonProperty("currentActiveInstances") int currentActiveInstances,
                                   @JsonProperty("deployInstanceCountPerStep") int deployInstanceCountPerStep,
                                   @JsonProperty("deployStepWaitTimeMs") long deployStepWaitTimeMs,
                                   @JsonProperty("stepComplete") boolean stepComplete,
                                   @JsonProperty("autoAdvanceDeploySteps") boolean autoAdvanceDeploySteps,
                                   @JsonProperty("failedDeployTasks") Set<SingularityTaskId> failedDeployTasks,
                                   @JsonProperty("timestamp") long timestamp) {
    this.targetActiveInstances = targetActiveInstances;
    this.currentActiveInstances = currentActiveInstances;
    this.deployInstanceCountPerStep = deployInstanceCountPerStep;
    this.deployStepWaitTimeMs = deployStepWaitTimeMs;
    this.stepComplete = stepComplete;
    this.autoAdvanceDeploySteps = autoAdvanceDeploySteps;
    this.failedDeployTasks = failedDeployTasks;
    this.timestamp = timestamp;
  }

  public int getTargetActiveInstances() {
    return targetActiveInstances;
  }

  public int getCurrentActiveInstances() {
    return currentActiveInstances;
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

  public Set<SingularityTaskId> getFailedDeployTasks() {
    return failedDeployTasks;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public SingularityDeployProgress withNewTargetInstances(int instances) {
    return new SingularityDeployProgress(instances, currentActiveInstances, deployInstanceCountPerStep, deployStepWaitTimeMs, false, autoAdvanceDeploySteps, failedDeployTasks, System.currentTimeMillis());
  }

  public SingularityDeployProgress withNewActiveInstances(int instances) {
    return new SingularityDeployProgress(targetActiveInstances, instances, deployInstanceCountPerStep, deployStepWaitTimeMs, false, autoAdvanceDeploySteps, failedDeployTasks, System.currentTimeMillis());
  }

  public SingularityDeployProgress withCompletedStep() {
    return new SingularityDeployProgress(targetActiveInstances, currentActiveInstances, deployInstanceCountPerStep, deployStepWaitTimeMs, true, autoAdvanceDeploySteps, failedDeployTasks, System.currentTimeMillis());
  }

  public SingularityDeployProgress withFailedTasks(Set<SingularityTaskId> failedTasks) {
    return new SingularityDeployProgress(targetActiveInstances, currentActiveInstances, deployInstanceCountPerStep, deployStepWaitTimeMs, false, autoAdvanceDeploySteps, failedTasks, System.currentTimeMillis());
  }

  @Override
  public String toString() {
    return "SingularityIncrementalDeployProgress{" +
      "targetActiveInstances=" + targetActiveInstances +
      ", currentActiveInstances=" + currentActiveInstances +
      ", deployInstanceCountPerStep=" + deployInstanceCountPerStep +
      ", deployStepWaitTimeMs=" + deployStepWaitTimeMs +
      ", stepComplete=" + stepComplete +
      ", autoAdvanceDeploySteps=" + autoAdvanceDeploySteps +
      ", failedDeployTasks=" + failedDeployTasks +
      ", timestamp=" + timestamp +
      '}';
  }
}
