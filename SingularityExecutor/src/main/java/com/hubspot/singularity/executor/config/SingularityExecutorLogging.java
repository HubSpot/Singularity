package com.hubspot.singularity.executor.config;

import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.InvalidSingularityTaskIdException;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseLogging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

@Singleton
public class SingularityExecutorLogging {

  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(SingularityExecutorLogging.class);

  private final SingularityRunnerBaseLogging baseLogging;

  @Inject
  public SingularityExecutorLogging(SingularityRunnerBaseLogging baseLogging) {
    this.baseLogging = baseLogging;
  }

  public Logger buildTaskLogger(String taskId, String executorId, String executorPid, String taskLogFile) {
    LOG.info("Building a task logger for {} pointing to {}", taskId, taskLogFile);

    LoggerContext context = new LoggerContext();

    context.setName(executorPid);

    baseLogging.prepareRootLogger(context);

    String loggerId = taskId;

    try {
      SingularityTaskId singularityTaskId = SingularityTaskId.valueOf(taskId);

      loggerId = String.format("%s.%s.%s.%s.%s", singularityTaskId.getRequestId(), singularityTaskId.getDeployId(), singularityTaskId.getStartedAt(),
          singularityTaskId.getInstanceNo(), executorId);

    } catch (InvalidSingularityTaskIdException e) {
      LOG.info("Handling non-SingularityTaskId %s", taskId);
    }

    Logger taskLogger = context.getLogger(loggerId);
    taskLogger.detachAndStopAllAppenders();

    if (baseLogging.getRootLogPath().isPresent()) {
      taskLogger.addAppender(baseLogging.buildFileAppender(context, baseLogging.getRootLogPath().get()));
    }
    taskLogger.addAppender(baseLogging.buildFileAppender(context, taskLogFile));

    context.start();

    return taskLogger;
  }

  public void stopTaskLogger(String taskId, Logger logger) {
    LOG.info("Stopping task logger for {}", taskId);

    try {
      logger.info("Task finished, stopping logger");

      logger.detachAndStopAllAppenders();

      logger.getLoggerContext().stop();
    } catch (Throwable t) {
      LOG.error("While closing task logger for {}", taskId, t);
    }
  }

}
