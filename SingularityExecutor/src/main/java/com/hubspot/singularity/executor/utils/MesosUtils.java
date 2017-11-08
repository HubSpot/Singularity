package com.hubspot.singularity.executor.utils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.mesos.v1.Protos.TaskState;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.hubspot.singularity.ExtendedTaskState;

public class MesosUtils {
  public static Path getTaskDirectoryPath(String taskId) {
    return Paths.get(getSafeTaskIdForDirectory(taskId)).toAbsolutePath();
  }

  public static String getSafeTaskIdForDirectory(String taskId) {
    return taskId.replace(":", "_");
  }

  public static String formatForLogging(Object object) {
    return object.toString().replace("\n", "").replaceAll("( )+", " ");
  }

  private static final Map<TaskState, ExtendedTaskState> map;
  static {
    map = Maps.newHashMapWithExpectedSize(ExtendedTaskState.values().length);
    for (ExtendedTaskState extendedTaskState : ExtendedTaskState.values()) {
      if (extendedTaskState.toTaskState().isPresent()) {

        map.put(TaskState.valueOf(extendedTaskState.toTaskState().get().name()), extendedTaskState);
      }
    }

    for (TaskState t : TaskState.values()) {
      if (map.get(t) == null) {
        throw new IllegalStateException("No ExtendedTaskState provided for TaskState " + t + ", you probably have incompatible versions of Mesos and Singularity.");
      }
    }
  }

  public static ExtendedTaskState fromTaskState(TaskState taskState) {
    ExtendedTaskState extendedTaskState = map.get(taskState);
    Preconditions.checkArgument(extendedTaskState != null, "No ExtendedTaskState for TaskState %s", taskState);
    return extendedTaskState;
  }
}
