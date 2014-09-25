package com.hubspot.singularity;

import org.apache.mesos.Protos.TaskState;

public enum ExtendedTaskState {

  TASK_STAGING("staging", false), TASK_STARTING("starting", false), TASK_RUNNING("running", false), TASK_CLEANING("cleaning", false), TASK_FINISHED("finished", true), TASK_FAILED("failed", true),
  TASK_KILLED("killed", true), TASK_LOST("lost", true), TASK_LOST_WHILE_DOWN("lost while down", true);

  private final String displayName;
  private final boolean isDone;

  private ExtendedTaskState(String displayName, boolean isDone) {
    this.displayName = displayName;
    this.isDone = isDone;
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

  public static ExtendedTaskState fromTaskState(TaskState taskState) {
    switch (taskState) {
      case TASK_FAILED:
        return TASK_FAILED;
      case TASK_FINISHED:
        return TASK_FINISHED;
      case TASK_KILLED:
        return TASK_KILLED;
      case TASK_STARTING:
        return TASK_STARTING;
      case TASK_STAGING:
        return TASK_STAGING;
      case TASK_LOST:
        return TASK_LOST;
      case TASK_RUNNING:
        return TASK_RUNNING;
    }

    throw new IllegalStateException(String.format("TaskState: %s not found", taskState));
  }

}
