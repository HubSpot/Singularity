package com.hubspot.singularity;

import com.google.common.base.Optional;

public enum RequestType {

  SERVICE(true, true, true), WORKER(true, true, true), SCHEDULED(false, true, false), ON_DEMAND(false, false, false), RUN_ONCE(false, false, false);

  private final boolean longRunning;
  private final boolean alwaysRunning;
  private final boolean deployable;

  private RequestType(boolean longRunning, boolean alwaysRunning, boolean deployable) {
    this.alwaysRunning = alwaysRunning;
    this.deployable = deployable;
    this.longRunning = longRunning;
  }

  public boolean isLongRunning() {
    return longRunning;
  }

  public boolean isAlwaysRunning() {
    return alwaysRunning;
  }

  public boolean isDeployable() {
    return deployable;
  }

  @Deprecated
  public static RequestType fromDaemonAndScheduleAndLoadBalanced(Optional<String> schedule, Optional<Boolean> daemon, Optional<Boolean> loadBalanced) {
    if (schedule.isPresent()) {
      return RequestType.SCHEDULED;
    }

    if (!daemon.or(true)) {
      return RequestType.ON_DEMAND;
    }

    if (loadBalanced.isPresent() && loadBalanced.get()) {
      return RequestType.SERVICE;
    }

    return RequestType.WORKER;
  }

}
