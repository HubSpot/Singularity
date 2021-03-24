package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Optional;

@Schema(
  description = "A set of instructions for how to roll out groups of instances for a new deploy"
)
public class CanaryDeploySettings {
  /**
   * For Load Balanced deploys, if false:
   * - Once all instances are healthy, a single LB update adding all new instances + removing all old instances will execute
   * - No rolling/incremental options are available
   * For Non Load Balanced Deploys, if true:
   * - All old deploy tasks will be marked as cleaning as soon as all new deploy tasks are healthy
   * - No rolling/incremental options are available
   *
   * If true, new deploy tasks will be spun up according to the settings below. It is possible for
   * new and old deploy tasks to be active/in the load balancer at once
   */
  private final boolean enableCanaryDeploy;

  /**
   * Use to determine how to decide that a deploy, or canary group is successful.
   * If using a canary rollout, triggers the start of the next instance group, otherwise
   * triggers the success of the deploy (and clean up of old instances)
   */
  private final DeployAcceptanceMode acceptanceMode;

  /**
   * Roll out this many instances at a time during a new deploy
   */
  private final int instanceGroupSize;

  /**
   * If using {@link DeployAcceptanceMode.TIMED}, wait this long between instance groups
   */
  private final long waitMillisBetweenGroups;

  /**
   * Allow this many tasks to fail and retry launch during each instance group
   */
  private final int allowedTasksFailuresPerGroup;

  /**
   * Run this many cycles of canary instance groups before launching all remaining instances
   */
  private final int canaryCycleCount;

  // TODO - max canary cycles - e.g. before skipping to all remaining instances

  public CanaryDeploySettings() {
    this(
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()
    );
  }

  @JsonCreator
  public CanaryDeploySettings(
    @JsonProperty("enableCanaryDeploy") Optional<Boolean> enableCanaryDeploy,
    @JsonProperty("instanceGroupSize") Optional<Integer> instanceGroupSize,
    @JsonProperty("acceptanceMode") Optional<DeployAcceptanceMode> acceptanceMode,
    @JsonProperty("waitMillisBetweenGroups") Optional<Long> waitMillisBetweenGroups,
    @JsonProperty(
      "allowedTasksFailuresPerGroup"
    ) Optional<Integer> allowedTasksFailuresPerGroup,
    @JsonProperty("canaryCycleCount") Optional<Integer> canaryCycleCount
  ) {
    this.enableCanaryDeploy = enableCanaryDeploy.orElse(false);
    this.instanceGroupSize = instanceGroupSize.orElse(1);
    this.acceptanceMode = acceptanceMode.orElse(DeployAcceptanceMode.NONE);
    this.waitMillisBetweenGroups = waitMillisBetweenGroups.orElse(0L);
    this.allowedTasksFailuresPerGroup = allowedTasksFailuresPerGroup.orElse(0);
    this.canaryCycleCount = canaryCycleCount.orElse(1);
  }

  public static CanaryDeploySettingsBuilder newbuilder() {
    return new CanaryDeploySettingsBuilder();
  }

  public CanaryDeploySettingsBuilder toBuilder() {
    CanaryDeploySettingsBuilder builder = new CanaryDeploySettingsBuilder();
    return builder
      .setEnableCanaryDeploy(enableCanaryDeploy)
      .setInstanceGroupSize(instanceGroupSize)
      .setAcceptanceMode(acceptanceMode)
      .setWaitMillisBetweenGroups(waitMillisBetweenGroups)
      .setAllowedTasksFailuresPerGroup(allowedTasksFailuresPerGroup)
      .setCanaryCycleCount(canaryCycleCount);
  }

  @Schema(
    description = "Determines if instances are launched and accepted in a rolling fashion or all at once",
    defaultValue = "false"
  )
  public boolean isEnableCanaryDeploy() {
    return enableCanaryDeploy;
  }

  @Schema(
    description = "Determines how to decide if an instance group has succeeded before moving on to the next",
    defaultValue = "NONE"
  )
  public DeployAcceptanceMode getAcceptanceMode() {
    return acceptanceMode;
  }

  @Schema(
    description = "Roll out this many instances at once (if atomicSwap is false)",
    defaultValue = "1"
  )
  public int getInstanceGroupSize() {
    return instanceGroupSize;
  }

  @Schema(
    description = "Wait this amount of time between instance groups (if atomicSwap is false and acceptanceMode is TIMED)",
    defaultValue = "0"
  )
  public long getWaitMillisBetweenGroups() {
    return waitMillisBetweenGroups;
  }

  @Schema(
    description = "Allow this many tasks to fail in each instance group before marking the overall deploy as failed",
    defaultValue = "0"
  )
  public int getAllowedTasksFailuresPerGroup() {
    return allowedTasksFailuresPerGroup;
  }

  @Schema(
    description = "Run this many canary instance groups of size `instanceGroupSize` before launching all remaining instances",
    defaultValue = "1"
  )
  public int getCanaryCycleCount() {
    return canaryCycleCount;
  }
}
