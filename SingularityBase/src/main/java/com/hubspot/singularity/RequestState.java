package com.hubspot.singularity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public enum RequestState {

  ACTIVE(true), DELETING(false), DELETED(false), PAUSED(false), SYSTEM_COOLDOWN(true), FINISHED(false), DEPLOYING_TO_UNPAUSE(true);

  private final boolean isRunnable;

  private RequestState(boolean isRunnable) {
    this.isRunnable = isRunnable;
  }

  public boolean isRunnable() {
    return isRunnable;
  }

}
