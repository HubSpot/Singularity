package com.hubspot.singularity.api.expiring;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.singularity.api.machines.MachineState;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents a future update to the state of a slave")
public class SingularityExpiringSlaveState extends SingularityExpiringMachineState {

  @JsonCreator
  public SingularityExpiringSlaveState(@JsonProperty("user") Optional<String> user,
                                       @JsonProperty("startMillis") long startMillis,
                                       @JsonProperty("actionId") String actionId,
                                       @JsonProperty("expiringAPIRequestObject") SingularityMachineChangeRequest machineChangeRequest,
                                       @JsonProperty("machineId") String machineId,
                                       @JsonProperty("revertToState") MachineState revertToState,
                                       @JsonProperty("killTasksOnDecommissionTimeout") Optional<Boolean> killTasksOnDecommissionTimeout) {
    super(user, startMillis, actionId, machineChangeRequest, machineId, revertToState, killTasksOnDecommissionTimeout);
  }
}
