package com.hubspot.singularity.expiring;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.api.SingularityMachineChangeRequest;

public class SingularityExpiringMachineState extends SingularityExpiringParent<SingularityMachineChangeRequest> {

  private final String machineId;
  private final Optional<MachineState> revertToState;
  private final boolean killTasksOnDecommissionTimeout;

  @JsonCreator
  public SingularityExpiringMachineState(Optional<String> user, long startMillis, String actionId, SingularityMachineChangeRequest machineChangeRequest,
    String machineId, Optional<MachineState> revertToState, Optional<Boolean> killTasksOnDecommissionTimeout) {
    super(machineChangeRequest, user, startMillis, actionId);
    this.machineId = machineId;
    this.revertToState = revertToState;
    this.killTasksOnDecommissionTimeout = killTasksOnDecommissionTimeout.or(false);
  }

  public String getMachineId() {
    return machineId;
  }

  public Optional<MachineState> getRevertToState() {
    return revertToState;
  }

  public boolean isKillTasksOnDecommissionTimeout() {
    return killTasksOnDecommissionTimeout;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("user", getUser())
      .add("startMillis", getStartMillis())
      .add("actionId", getActionId())
      .add("expiringAPIRequestObject", getExpiringAPIRequestObject())
      .add("revertToState", revertToState)
      .add("killTasksOnDecommissionTimeout", killTasksOnDecommissionTimeout)
      .toString();
  }
}
