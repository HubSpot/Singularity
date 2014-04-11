package com.hubspot.singularity.executor.utils;

import org.apache.mesos.ExecutorDriver;
import org.apache.mesos.Protos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.hubspot.singularity.executor.SingularityExecutorRunner;

public class ExecutorUtils {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityExecutorRunner.class);

  private ExecutorUtils() { }

  public static void sendStatusUpdate(ExecutorDriver driver, Protos.TaskInfo taskInfo, Protos.TaskState taskState, String message) {
    LOG.info("Sending status update \"{}\" ({}) for task {}", message, taskState.name(), taskInfo.getTaskId().getValue());

    try {
      final Protos.TaskStatus.Builder builder = Protos.TaskStatus.newBuilder()
          .setTaskId(taskInfo.getTaskId())
          .setState(taskState)
          .setMessage(message);
    
      driver.sendStatusUpdate(builder.build());
    } catch (Throwable t) {
      LOG.error("While sending status update", t);
      throw Throwables.propagate(t);
    }
  }

}
