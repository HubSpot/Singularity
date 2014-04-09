package com.hubspot.singularity;

import org.apache.mesos.Protos.TaskState;

public enum ExtendedTaskState {

  TASK_STAGING("staging", false, false), TASK_STARTING("starting", false, false), TASK_RUNNING("running", false, false), TASK_CLEANING("cleaning", false, false),
  TASK_FINISHED("finished", true, false), TASK_FAILED("failed", true, true), TASK_KILLED("killed", true, false), TASK_LOST("lost", true, true), TASK_LOST_WHILE_DOWN("lost while down", true, true);
  
  private final String displayName;
  private final boolean isDone;
  private final boolean isFailed;
  
  private ExtendedTaskState(String displayName, boolean isDone, boolean isFailed) {
    this.displayName = displayName;
    this.isDone = isDone;
    this.isFailed = isFailed;
  }

  public String getDisplayName() {
    return displayName;
  }
  
  public boolean isDone() {
    return isDone;
  }
  
  public boolean isFailed() {
    return isFailed;
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
    default:
      throw new IllegalStateException(String.format("TaskState: %s not found", taskState));
    }
  }
  
}