package com.hubspot.singularity.runner.base.config;

import java.util.Map.Entry;
import java.util.Properties;

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
  private final Properties properties;
  
  @Inject
  public SingularityRunnerBaseLogging(@Named(SingularityRunnerBaseConfigurationLoader.ROOT_LOG_PATH) String rootLogPath, @Named(SingularityRunnerBaseConfigurationLoader.LOGGING_PATTERN) String loggingPattern, Properties properties) {
    this.rootLogPath = rootLogPath;
    this.loggingPattern = loggingPattern;
    this.properties = properties;
    
    Logger rootLogger = configureRootLogger();
    printProperties(rootLogger);
  }
  
  public void printProperties(Logger rootLogger) {
    rootLogger.debug("Loaded {} properties", properties.size());
    
    for (Entry<Object, Object> entries : properties.entrySet()) {
      rootLogger.debug("  {} -> {}", entries.getKey(), entries.getValue());
    }
  }
  
  public String getRootLogPath() {
    return rootLogPath;
  }
  
  public Logger prepareRootLogger(LoggerContext context) {
    Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
    
    rootLogger.detachAndStopAllAppenders();
    
    return rootLogger;
  }
  
  public Logger configureRootLogger() {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

    Logger rootLogger = prepareRootLogger(context);
    
    rootLogger.addAppender(buildFileAppender(context, rootLogPath));
    
    return rootLogger;
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
