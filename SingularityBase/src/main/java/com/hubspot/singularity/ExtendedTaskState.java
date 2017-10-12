package com.hubspot.singularity;

import java.util.Map;

import org.apache.mesos.v1.Protos.TaskState;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

public enum ExtendedTaskState {

  TASK_LAUNCHED("launched", false, Optional.absent()), TASK_STAGING("staging", false, Optional.of(TaskState.TASK_STAGING)),
  TASK_STARTING("starting", false, Optional.of(TaskState.TASK_STARTING)), TASK_RUNNING("running", false, Optional.of(TaskState.TASK_RUNNING)),
  TASK_CLEANING("cleaning", false, Optional.absent()), TASK_KILLING("killing", false, Optional.of(TaskState.TASK_KILLING)), TASK_FINISHED("finished", true, Optional.of(TaskState.TASK_FINISHED)),
  TASK_FAILED("failed", true, Optional.of(TaskState.TASK_FAILED)), TASK_KILLED("killed", true, Optional.of(TaskState.TASK_KILLED)),
  TASK_LOST("lost", true, Optional.of(TaskState.TASK_LOST)), TASK_LOST_WHILE_DOWN("lost", true, Optional.<TaskState>absent()), TASK_ERROR("error", true, Optional.of(TaskState.TASK_ERROR)),
  TASK_DROPPED("dropped", true, Optional.of(TaskState.TASK_DROPPED)), TASK_GONE("gone", true, Optional.of(TaskState.TASK_GONE)), TASK_UNREACHABLE("unreachable", true, Optional.of(TaskState.TASK_UNREACHABLE)),
  TASK_GONE_BY_OPERATOR("goneByOperator", true, Optional.of(TaskState.TASK_GONE_BY_OPERATOR)), TASK_UNKNOWN("dropped", true, Optional.of(TaskState.TASK_UNKNOWN));

  private static final Map<TaskState, ExtendedTaskState> map;
  static {
    map = Maps.newHashMapWithExpectedSize(ExtendedTaskState.values().length);
    for (ExtendedTaskState extendedTaskState : ExtendedTaskState.values()) {
      if (extendedTaskState.toTaskState().isPresent()) {
        map.put(extendedTaskState.toTaskState().get(), extendedTaskState);
      }
    }

    for (TaskState t : TaskState.values()) {
      if (map.get(t) == null) {
        throw new IllegalStateException("No ExtendedTaskState provided for TaskState " + t + ", you probably have incompatible versions of Mesos and Singularity.");
      }
    }
  }

  private final String displayName;
  private final boolean isDone;
  private final Optional<TaskState> taskState;

  ExtendedTaskState(String displayName, boolean isDone, Optional<TaskState> taskState) {
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

  public Optional<TaskState> toTaskState() {
    return taskState;
  }

  public static ExtendedTaskState fromTaskState(TaskState taskState) {
    ExtendedTaskState extendedTaskState = map.get(taskState);
    Preconditions.checkArgument(extendedTaskState != null, "No ExtendedTaskState for TaskState %s", taskState);
    return extendedTaskState;
  }

}
