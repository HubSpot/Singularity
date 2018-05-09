package com.hubspot.singularity.expiring;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.api.SingularityMachineChangeRequest;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    title = "Represents a future update to the state of a rack or slave",
    subTypes = {
        SingularityExpiringSlaveState.class
    }
)
public class SingularityExpiringMachineState extends SingularityExpiringParent<SingularityMachineChangeRequest> {

  private final String machineId;
  private final MachineState revertToState;
  private final boolean killTasksOnDecommissionTimeout;

  @JsonCreator
  public SingularityExpiringMachineState(@JsonProperty("user") Optional<String> user,
                                         @JsonProperty("startMillis") long startMillis,
                                         @JsonProperty("actionId") String actionId,
                                         @JsonProperty("expiringAPIRequestObject") SingularityMachineChangeRequest machineChangeRequest,
                                         @JsonProperty("machineId") String machineId,
                                         @JsonProperty("revertToState") MachineState revertToState,
                                         @JsonProperty("killTasksOnDecommissionTimeout") Optional<Boolean> killTasksOnDecommissionTimeout) {
    super(machineChangeRequest, user, startMillis, actionId);
    this.machineId = machineId;
    this.revertToState = revertToState;
    this.killTasksOnDecommissionTimeout = killTasksOnDecommissionTimeout.or(false);
  }

  @Schema(description = "Id of the machine being updated")
  public String getMachineId() {
    return machineId;
  }

  @Schema(description = "State the machine will transition to")
  public MachineState getRevertToState() {
    return revertToState;
  }

  @Schema(
      title = "if true, kill all remaining tasks on the slave if the decommission has timed out",
      defaultValue = "false"
  )
  public boolean isKillTasksOnDecommissionTimeout() {
    return killTasksOnDecommissionTimeout;
  }

  @Override
  public String toString() {
    return "SingularityExpiringMachineState{" +
        "machineId='" + machineId + '\'' +
        ", revertToState=" + revertToState +
        ", killTasksOnDecommissionTimeout=" + killTasksOnDecommissionTimeout +
        "} " + super.toString();
  }
}
