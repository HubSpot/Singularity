package com.hubspot.singularity.runner.base.config;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class SingularityRunnerBaseLogging {

  private final String rootLogPath;
  private final String hubSpotLogLevel;
  private final String loggingPattern;
  private final Properties properties;
  
  @Inject
  public SingularityRunnerBaseLogging(@Named(SingularityRunnerBaseConfigurationLoader.ROOT_LOG_PATH) String rootLogPath, @Named(SingularityRunnerBaseConfigurationLoader.LOGGING_PATTERN) String loggingPattern, 
      @Named(SingularityRunnerBaseConfigurationLoader.ROOT_LOG_LEVEL) String rootLogLevel, @Named(SingularityRunnerBaseConfigurationLoader.HUBSPOT_LOG_LEVEL) String hubSpotLogLevel, Properties properties) {
    this.rootLogPath = rootLogPath;
    this.loggingPattern = loggingPattern;
    this.properties = properties;
    this.hubSpotLogLevel = hubSpotLogLevel;
    
    Logger rootLogger = configureRootLogger(rootLogLevel);
    printProperties(rootLogger);
  }
  
  public void printProperties(Logger rootLogger) {
    rootLogger.debug("Loaded {} properties", properties.size());
    
    List<String> strKeys = Lists.newArrayListWithCapacity(properties.size());
    for (Object object : properties.keySet()) {
      strKeys.add(object.toString());
    }
    Collections.sort(strKeys);
    
    for (String key : strKeys) {
      rootLogger.debug("  {} -> {}", key, properties.get(key));
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
  
  public Logger configureRootLogger(String rootLogLevel) {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

    Logger rootLogger = prepareRootLogger(context);

    rootLogger.setLevel(Level.toLevel(rootLogLevel));
    
    Logger hubSpotLogger = context.getLogger("com.hubspot");
    
    hubSpotLogger.setLevel(Level.toLevel(hubSpotLogLevel));
    
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
