package com.hubspot.singularity.api.expiring;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.singularity.api.machines.MachineState;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Settings related to changing the state of a rack or slave")
public class SingularityMachineChangeRequest extends SingularityExpiringRequestParent {

  private final Optional<MachineState> revertToState;
  private final boolean killTasksOnDecommissionTimeout;

  @Deprecated
  public SingularityMachineChangeRequest(Optional<String> message) {
    this(Optional.empty(), Optional.empty(), message, Optional.empty(), Optional.empty());
  }

  public static SingularityMachineChangeRequest empty() {
    return new SingularityMachineChangeRequest(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
  }

  @JsonCreator
  public SingularityMachineChangeRequest(@JsonProperty("durationMillis") Optional<Long> durationMillis,
                                         @JsonProperty("actionId") Optional<String> actionId,
                                         @JsonProperty("message") Optional<String> message,
                                         @JsonProperty("revertToState") Optional<MachineState> revertToState,
                                         @JsonProperty("killTasksOnDecommissionTimeout") Optional<Boolean> killTasksOnDecommissionTimeout) {
    super(durationMillis, actionId, message);
    this.revertToState = revertToState;
    this.killTasksOnDecommissionTimeout = killTasksOnDecommissionTimeout.orElse(false);
  }

  @Schema(description = "If a durationMillis is specified, return to this state when time has elapsed", nullable = true)
  public Optional<MachineState> getRevertToState() {
    return revertToState;
  }

  @Schema(description = "If a machine has not successfully decommissioned in durationMillis, kill the remaining tasks on the machine")
  public boolean isKillTasksOnDecommissionTimeout() {
    return killTasksOnDecommissionTimeout;
  }

  @Override
  public String toString() {
    return "SingularityMachineChangeRequest{" +
        "revertToState=" + revertToState +
        ", killTasksOnDecommissionTimeout=" + killTasksOnDecommissionTimeout +
        "} " + super.toString();
  }
}
