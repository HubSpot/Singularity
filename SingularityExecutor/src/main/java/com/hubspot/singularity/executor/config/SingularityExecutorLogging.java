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
  
  @Inject
  public SingularityExecutorLogging(@Named(SingularityExecutorModule.ROOT_LOG_PATH) String rootLogPath) {
    this.rootLogPath = rootLogPath;
    
    configureRootLogger();
  }
  
  private Logger prepareRootLoggerWithStaticExecutorLogger(LoggerContext context) {
    Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
    
    rootLogger.detachAndStopAllAppenders();
    
    rootLogger.addAppender(buildFileAppender(context, rootLogPath));
  
    return rootLogger;
  }
  
  public Logger buildTaskLogger(String taskId, String taskLogFile) {
    LOG.info("Building a task logger for {} pointing to {}", taskId, taskLogFile);
    
    LoggerContext context = new LoggerContext();
    
    Logger rootLogger = prepareRootLoggerWithStaticExecutorLogger(context);
 
    rootLogger.addAppender(buildFileAppender(context, taskLogFile));

    context.start();
    
    return rootLogger;
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

    prepareRootLoggerWithStaticExecutorLogger(context);
  }
  
  private FileAppender<ILoggingEvent> buildFileAppender(LoggerContext context, String file) {
    FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
    fileAppender.setFile(file);
    fileAppender.setContext(context);
    fileAppender.setPrudent(true);
        
    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
    encoder.setContext(context);
    encoder.setPattern("%-4relative [%thread] %-5level %logger{35} - %msg%n");
    encoder.start();
    
    fileAppender.setEncoder(encoder);
    fileAppender.start();
    
    return fileAppender;
  }
  
}
