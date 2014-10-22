package com.hubspot.singularity;

public enum RequestState {

  ACTIVE(true), DELETED(false), PAUSED(false), SYSTEM_COOLDOWN(true), FINISHED(false), DEPLOYING_TO_UNPAUSE(true);

  private final boolean isRunnable;

  private RequestState(boolean isRunnable) {
    this.isRunnable = isRunnable;
  }

  public boolean isRunnable() {
    return isRunnable;
  }

}
