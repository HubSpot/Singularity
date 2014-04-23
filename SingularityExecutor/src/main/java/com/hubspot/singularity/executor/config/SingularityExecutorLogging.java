package com.hubspot.singularity.executor.config;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class SingularityExecutorLogging {

  private final static org.slf4j.Logger LOG = LoggerFactory.getLogger(SingularityExecutorLogging.class);
  
  private final String rootLogPath;
  private final String loggingPattern;
  
  @Inject
  public SingularityExecutorLogging(@Named(SingularityExecutorModule.ROOT_LOG_PATH) String rootLogPath, @Named(SingularityExecutorModule.LOGGING_PATTERN) String loggingPattern) {
    this.rootLogPath = rootLogPath;
    this.loggingPattern = loggingPattern;
    
    configureRootLogger();
  }
  
  private Logger prepareRootLogger(LoggerContext context) {
    Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
    
    rootLogger.detachAndStopAllAppenders();
    
    return rootLogger;
  }
  
  public Logger buildTaskLogger(String taskId, String taskLogFile) {
    LOG.info("Building a task logger for {} pointing to {}", taskId, taskLogFile);
    
    LoggerContext context = new LoggerContext();
    
    prepareRootLogger(context);
    
    Logger taskLogger = context.getLogger(taskId);
    taskLogger.detachAndStopAllAppenders();
    
    taskLogger.addAppender(buildFileAppender(context, rootLogPath));
    taskLogger.addAppender(buildFileAppender(context, taskLogFile));

    context.start();
    
    return taskLogger;
  }
  
  public void stopTaskLogger(String taskId, Logger logger) {
    LOG.info("Stopping task logger for {}", taskId);
    
    try {
      logger.detachAndStopAllAppenders();
    
      logger.getLoggerContext().stop();
    } catch (Throwable t) {
      LOG.error("While closing task logger for {}", taskId, t);
    }
  }
  
  private void configureRootLogger() {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

    Logger rootLogger = prepareRootLogger(context);
    
    rootLogger.addAppender(buildFileAppender(context, rootLogPath));
  }
  
  private FileAppender<ILoggingEvent> buildFileAppender(LoggerContext context, String file) {
    FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
    fileAppender.setFile(file);
    fileAppender.setContext(context);
    fileAppender.setPrudent(true);
        
    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
    encoder.setContext(context);
    encoder.setPattern(loggingPattern);
    encoder.start();
        
    fileAppender.setEncoder(encoder);
    fileAppender.start();
    
    return fileAppender;
  }
  
}
