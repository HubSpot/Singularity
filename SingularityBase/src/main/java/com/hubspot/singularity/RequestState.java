package com.hubspot.singularity;

public enum RequestState {

  ACTIVE(true), DELETED(false), PAUSED(false), SYSTEM_COOLDOWN(true);
  
  private final boolean isRunnable;

  private RequestState(boolean isRunnable) {
    this.isRunnable = isRunnable;
  }

  public boolean isRunnable() {
    return isRunnable;
  }
  
}
