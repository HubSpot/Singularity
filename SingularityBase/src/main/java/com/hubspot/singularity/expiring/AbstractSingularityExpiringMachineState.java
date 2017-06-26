package com.hubspot.singularity.expiring;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.api.SingularityMachineChangeRequest;

@Immutable
@SingularityStyle
public abstract class AbstractSingularityExpiringMachineState extends SingularityExpiringParent<SingularityMachineChangeRequest> {

  public abstract Optional<String> getUser();

  public abstract long getStartMillis();

  public abstract String getActionId();

  public abstract SingularityMachineChangeRequest getExpiringAPIRequestObject();

  public abstract String getMachineId();

  public abstract MachineState getRevertToState();

  @Default
  public boolean isKillTasksOnDecommissionTimeout() {
    return false;
  }
}
