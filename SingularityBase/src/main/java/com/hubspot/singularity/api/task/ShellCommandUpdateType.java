package com.hubspot.singularity.api.task;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public enum ShellCommandUpdateType {
  INVALID(true), ACKED(false), STARTED(false), FINISHED(true), FAILED(true);

  private final boolean finished;

  ShellCommandUpdateType(boolean finished) {
    this.finished = finished;
  }

  public boolean isFinished() {
    return finished;
  }
}
