package com.hubspot.singularity.logwatcher.impl;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.hubspot.singularity.logwatcher.LogForwarder;
import com.hubspot.singularity.logwatcher.SimpleStore;

public class SingularityLogWatcherImplModule extends AbstractModule {

  @Override
  protected void configure() {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

    Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
    
    rootLogger.setLevel(Level.ALL);
    
    bind(SimpleStore.class).to(FileBasedSimpleStore.class).in(Scopes.SINGLETON);
    bind(LogForwarder.class).to(FluentdLogForwarder.class).in(Scopes.SINGLETON);
  }

}
