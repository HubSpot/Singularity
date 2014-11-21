package com.hubspot.singularity;

import com.google.common.base.Optional;

public enum RequestType {

  SERVICE(true, true), WORKER(true, true), SCHEDULED(false, false), ON_DEMAND(false, false), RUN_ONCE(false, true);

  private final boolean alwaysRunning;
  private final boolean deployable;

  private RequestType(boolean alwaysRunning, boolean deployable) {
    this.alwaysRunning = alwaysRunning;
    this.deployable = deployable;
  }

  public boolean isAlwaysRunning() {
    return alwaysRunning;
  }

  public boolean isDeployable() {
    return deployable;
  }

  @Deprecated
  public static RequestType fromDaemonAndSchedule(Optional<String> schedule, Optional<Boolean> daemon) {
    if (schedule.isPresent()) {
      return RequestType.SCHEDULED;
    }

    if (!daemon.or(Boolean.TRUE).booleanValue()) {
      return RequestType.ON_DEMAND;
    }

    return RequestType.WORKER;
  }

}
