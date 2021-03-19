package com.hubspot.singularity;

import java.util.Optional;

public class CanaryDeploySettingsBuilder {
  private Optional<Boolean> atomicSwap = Optional.empty();
  private Optional<DeployAcceptanceMode> acceptanceMode = Optional.empty();
  private Optional<Integer> instanceGroupSize = Optional.empty();
  private Optional<Long> waitMillisBetweenGroups = Optional.empty();
  private Optional<Integer> allowedTasksFailuresPerGroup = Optional.empty();
  private Optional<Integer> canaryCycleCount = Optional.empty();

  public CanaryDeploySettingsBuilder() {}

  public CanaryDeploySettingsBuilder setAtomicSwap(boolean atomicSwap) {
    this.atomicSwap = Optional.of(atomicSwap);
    return this;
  }

  public CanaryDeploySettingsBuilder setAcceptanceMode(
    DeployAcceptanceMode acceptanceMode
  ) {
    this.acceptanceMode = Optional.of(acceptanceMode);
    return this;
  }

  public CanaryDeploySettingsBuilder setInstanceGroupSize(int instanceGroupSize) {
    this.instanceGroupSize = Optional.of(instanceGroupSize);
    return this;
  }

  public CanaryDeploySettingsBuilder setWaitMillisBetweenGroups(
    long waitMillisBetweenGroups
  ) {
    this.waitMillisBetweenGroups = Optional.of(waitMillisBetweenGroups);
    return this;
  }

  public CanaryDeploySettingsBuilder setAllowedTasksFailuresPerGroup(
    int allowedTasksFailuresPerGroup
  ) {
    this.allowedTasksFailuresPerGroup = Optional.of(allowedTasksFailuresPerGroup);
    return this;
  }

  public CanaryDeploySettingsBuilder setCanaryCycleCount(int canaryCycleCount) {
    this.canaryCycleCount = Optional.of(canaryCycleCount);
    return this;
  }

  public CanaryDeploySettings build() {
    return new CanaryDeploySettings(
      atomicSwap,
      instanceGroupSize,
      acceptanceMode,
      waitMillisBetweenGroups,
      allowedTasksFailuresPerGroup,
      canaryCycleCount
    );
  }
}
