package com.hubspot.singularity.expiring;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.api.SingularityMachineChangeRequest;

public class SingularityExpiringRackState extends SingularityExpiringMachineState {

  @JsonCreator
  public SingularityExpiringRackState(@JsonProperty("user") Optional<String> user,
                                      @JsonProperty("startMillis") long startMillis,
                                      @JsonProperty("actionId") String actionId,
                                      @JsonProperty("expiringAPIRequestObject") SingularityMachineChangeRequest machineChangeRequest,
                                      @JsonProperty("machineId") String machineId,
                                      @JsonProperty("revertToState") MachineState revertToState,
                                      @JsonProperty("killTasksOnDecommissionTimeout") Optional<Boolean> killTasksOnDecommissionTimeout) {
    super(user, startMillis, actionId, machineChangeRequest, machineId, revertToState, killTasksOnDecommissionTimeout);
  }
}
