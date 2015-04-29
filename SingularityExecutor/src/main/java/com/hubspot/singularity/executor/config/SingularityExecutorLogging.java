package com.hubspot.singularity.executor.config;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseLogging;

@Singleton
public class SingularityExecutorLogging {

  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(SingularityExecutorLogging.class);

  private final SingularityRunnerBaseLogging baseLogging;

  @Inject
  public SingularityExecutorLogging(SingularityRunnerBaseLogging baseLogging) {
    this.baseLogging = baseLogging;
  }

  public Logger buildTaskLogger(String taskId, String taskLogFile) {
    LOG.info("Building a task logger for {} pointing to {}", taskId, taskLogFile);

    LoggerContext context = new LoggerContext();

    baseLogging.prepareRootLogger(context);

    Logger taskLogger = context.getLogger(taskId);
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
