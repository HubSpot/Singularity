package com.hubspot.singularity;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Describes the progress a deploy has made")
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

  @Schema(description = "The desired number of instances for the current deploy step")
  public int getTargetActiveInstances() {
    return targetActiveInstances;
  }

  @Schema(description = "The current number of active tasks for this deploy")
  public int getCurrentActiveInstances() {
    return currentActiveInstances;
  }

  @Schema(description = "The number of instances to increment each time a deploy step completes")
  public int getDeployInstanceCountPerStep() {
    return deployInstanceCountPerStep;
  }

  @Schema(description = "`true` if the current deploy step has completed")
  public boolean isStepComplete() {
    return stepComplete;
  }

  @Schema(description = "If `true` automatically move to the next deploy step when reaching the target active instances for the current step")
  public boolean isAutoAdvanceDeploySteps() {
    return autoAdvanceDeploySteps;
  }

  @Schema(description = "The time to wait between deploy steps in milliseconds")
  public long getDeployStepWaitTimeMs() {
    return deployStepWaitTimeMs;
  }

  @Schema(description = "Tasks for this deploy that have failed so far")
  public Set<SingularityTaskId> getFailedDeployTasks() {
    return failedDeployTasks;
  }

  @Schema(description = "The timestamp of this deploy progress update")
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
