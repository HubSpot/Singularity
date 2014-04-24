package com.hubspot.singularity.runner.base.config;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class SingularityRunnerBaseLogging {

  private final String rootLogPath;
  private final String loggingPattern;
  
  @Inject
  public SingularityRunnerBaseLogging(@Named(SingularityRunnerBaseConfigurationLoader.ROOT_LOG_PATH) String rootLogPath, @Named(SingularityRunnerBaseConfigurationLoader.LOGGING_PATTERN) String loggingPattern) {
    this.rootLogPath = rootLogPath;
    this.loggingPattern = loggingPattern;
    
    configureRootLogger();
  }
  
  public String getRootLogPath() {
    return rootLogPath;
  }
  
  public Logger prepareRootLogger(LoggerContext context) {
    Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
    
    rootLogger.detachAndStopAllAppenders();
    
    return rootLogger;
  }
  
  public void configureRootLogger() {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

    Logger rootLogger = prepareRootLogger(context);
    
    rootLogger.addAppender(buildFileAppender(context, rootLogPath));
  }
  
  public FileAppender<ILoggingEvent> buildFileAppender(LoggerContext context, String file) {
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
