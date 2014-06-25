package com.hubspot.singularity.executor.utils;

import org.apache.mesos.ExecutorDriver;
import org.apache.mesos.Protos;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;

public class ExecutorUtils {

  private final SingularityExecutorConfiguration configuration;
  
  @Inject
  public ExecutorUtils(SingularityExecutorConfiguration configuration) {
    this.configuration = configuration;
  }
  
  public void sendStatusUpdate(ExecutorDriver driver, Protos.TaskInfo taskInfo, Protos.TaskState taskState, String message, Logger taskLogger) {
    taskLogger.info("Sending status update \"{}\" ({})", message, taskState.name());

    message = message.substring(0, Math.min(configuration.getMaxTaskMessageLength(), message.length()));
    
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
