package com.hubspot.singularity;

import java.util.Set;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import com.hubspot.immutables.style.SingularityStyle;

@Immutable
@SingularityStyle
public abstract class AbstractSingularityDeployProgress {

  public abstract int getTargetActiveInstances();

  public abstract int getCurrentActiveInstances();

  public abstract int getDeployInstanceCountPerStep();

  public abstract long getDeployStepWaitTimeMs();

  public abstract boolean isStepComplete();

  public abstract boolean isAutoAdvanceDeploySteps();

  public abstract Set<SingularityTaskId> getFailedDeployTasks();

  @Default
  public long getTimestamp() {
    return System.currentTimeMillis();
  }

  public SingularityDeployProgress withNewTargetInstances(int instances) {
    return SingularityDeployProgress.builder().from(this)
        .setTargetActiveInstances(instances)
        .setStepComplete(false)
        .setTimestamp(System.currentTimeMillis())
        .build();
  }

  public SingularityDeployProgress withNewActiveInstances(int instances) {
    return SingularityDeployProgress.builder().from(this)
        .setCurrentActiveInstances(instances)
        .setStepComplete(false)
        .setTimestamp(System.currentTimeMillis())
        .build();
  }

  public SingularityDeployProgress withCompletedStep() {
    return SingularityDeployProgress.builder().from(this)
        .setStepComplete(true)
        .setTimestamp(System.currentTimeMillis())
        .build();
  }

  public SingularityDeployProgress withFailedTasks(Set<SingularityTaskId> failedTasks) {
    return SingularityDeployProgress.builder().from(this)
        .setFailedDeployTasks(failedTasks)
        .setStepComplete(false)
        .setTimestamp(System.currentTimeMillis())
        .build();
  }
}
