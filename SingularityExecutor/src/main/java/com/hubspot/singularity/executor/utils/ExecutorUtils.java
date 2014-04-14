package com.hubspot.singularity.executor.utils;

import org.apache.mesos.ExecutorDriver;
import org.apache.mesos.Protos;
import org.slf4j.Logger;

public class ExecutorUtils {

  public void sendStatusUpdate(ExecutorDriver driver, Protos.TaskInfo taskInfo, Protos.TaskState taskState, String message, Logger taskLogger) {
    taskLogger.info("Sending status update \"{}\" ({}) for task {}", message, taskState.name(), taskInfo.getTaskId().getValue());

    try {
      final Protos.TaskStatus.Builder builder = Protos.TaskStatus.newBuilder()
          .setTaskId(taskInfo.getTaskId())
          .setState(taskState)
          .setMessage(message);
    
      driver.sendStatusUpdate(builder.build());
    } catch (Throwable t) {
      taskLogger.error("While sending status update", t);
    }
  }

}
