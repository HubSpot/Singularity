package com.hubspot.singularity;

import com.google.common.base.Optional;

public abstract class SingularityMachineAbstraction<T extends SingularityMachineAbstraction<T>> {

  private final String id;
  private final long firstSeenAt;
  private final SingularityMachineStateHistoryUpdate currentState;

  public SingularityMachineAbstraction(String id) {
    this(id, System.currentTimeMillis(), new SingularityMachineStateHistoryUpdate(id, MachineState.ACTIVE, System.currentTimeMillis(), Optional.<String> absent()));
  }

  public SingularityMachineAbstraction(String id, long firstSeenAt, SingularityMachineStateHistoryUpdate currentState) {
    this.id = id;
    this.currentState = currentState;
    this.firstSeenAt = firstSeenAt;
  }

  public String getId() {
    return id;
  }

  public long getFirstSeenAt() {
    return firstSeenAt;
  }

  public SingularityMachineStateHistoryUpdate getCurrentState() {
    return currentState;
  }

  public abstract T changeState(SingularityMachineStateHistoryUpdate newState);

}
