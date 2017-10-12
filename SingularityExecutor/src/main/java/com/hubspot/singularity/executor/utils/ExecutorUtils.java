package com.hubspot.singularity.executor.utils;

import org.apache.mesos.ExecutorDriver;
import org.apache.mesos.Protos;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class ExecutorUtils {

  private final SingularityExecutorConfiguration configuration;

  @Inject
  public ExecutorUtils(SingularityExecutorConfiguration configuration) {
    this.configuration = configuration;
  }

  @SuppressFBWarnings("DM_EXIT")
  public void sendStatusUpdate(ExecutorDriver driver, Protos.TaskID taskID, Protos.TaskState taskState, String message, Logger logger) {
    logger.info("Sending status update \"{}\" ({})", message, taskState.name());

    message = message.substring(0, Math.min(configuration.getMaxTaskMessageLength(), message.length()));

    try {
      final Protos.TaskStatus.Builder builder = Protos.TaskStatus.newBuilder()
          .setTaskId(taskID)
          .setState(taskState)
          .setMessage(message);

      driver.sendStatusUpdate(builder.build());
    } catch (Throwable t) {
      try {
        logger.error("Exception while sending status updates, exiting", t);
      } finally {
        System.exit(4);
      }
    }
  }

}
