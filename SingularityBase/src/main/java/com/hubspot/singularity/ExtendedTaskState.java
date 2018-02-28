package com.hubspot.singularity;

import com.google.common.base.Optional;
import com.hubspot.mesos.protos.MesosTaskState;

public enum ExtendedTaskState {

  TASK_LAUNCHED("launched", false, Optional.absent()), TASK_STAGING("staging", false, Optional.of(MesosTaskState.TASK_STAGING)),
  TASK_STARTING("starting", false, Optional.of(MesosTaskState.TASK_STARTING)), TASK_RUNNING("running", false, Optional.of(MesosTaskState.TASK_RUNNING)),
  TASK_CLEANING("cleaning", false, Optional.absent()), TASK_KILLING("killing", false, Optional.of(MesosTaskState.TASK_KILLING)), TASK_FINISHED("finished", true, Optional.of(MesosTaskState.TASK_FINISHED)),
  TASK_FAILED("failed", true, Optional.of(MesosTaskState.TASK_FAILED)), TASK_KILLED("killed", true, Optional.of(MesosTaskState.TASK_KILLED)),
  TASK_LOST("lost", true, Optional.of(MesosTaskState.TASK_LOST)), TASK_LOST_WHILE_DOWN("lost", true, Optional.absent()), TASK_ERROR("error", true, Optional.of(MesosTaskState.TASK_ERROR)),
  TASK_DROPPED("dropped", true, Optional.of(MesosTaskState.TASK_DROPPED)), TASK_GONE("gone", true, Optional.of(MesosTaskState.TASK_GONE)), TASK_UNREACHABLE("unreachable", true, Optional.of(MesosTaskState.TASK_UNREACHABLE)),
  TASK_GONE_BY_OPERATOR("goneByOperator", true, Optional.of(MesosTaskState.TASK_GONE_BY_OPERATOR)), TASK_UNKNOWN("dropped", true, Optional.of(MesosTaskState.TASK_UNKNOWN));

  private final String displayName;
  private final boolean isDone;
  private final Optional<MesosTaskState> taskState;

  ExtendedTaskState(String displayName, boolean isDone, Optional<MesosTaskState> taskState) {
    this.displayName = displayName;
    this.isDone = isDone;
    this.taskState = taskState;
  }

  public String getDisplayName() {
    return displayName;
  }

  public boolean isDone() {
    return isDone;
  }

  public boolean isFailed() {
    return this == TASK_FAILED;
  }

  public boolean isSuccess() {
    return this == TASK_FINISHED;
  }

  public Optional<MesosTaskState> toTaskState() {
    return taskState;
  }
}
