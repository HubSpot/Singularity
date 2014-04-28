package com.hubspot.singularity.logwatcher.config.test;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

import com.google.inject.AbstractModule;
import com.hubspot.singularity.logwatcher.LogForwarder;
import com.hubspot.singularity.logwatcher.SimpleStore;
import com.hubspot.singularity.logwatcher.impl.FileBasedSimpleStore;

public class SingularityLogWatcherTestModule extends AbstractModule {

  private final String[] args;
  
  public SingularityLogWatcherTestModule(String[] args) {
    this.args = args;
  }
  
  @Override
  protected void configure() {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

    Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
    
    rootLogger.setLevel(Level.ALL);
    
//    List<TailMetadata> list = Lists.newArrayList();
//    for (int i = 0; i < args.length; i += 2) {
//      TailMetadata tail = new TailMetadata(args[i], args[i + 1], Collections.<String, String> emptyMap(), false);
//      list.add(tail);
//    }
    
    bind(SimpleStore.class).to(FileBasedSimpleStore.class);
    bind(LogForwarder.class).toInstance(new LogLogForwarder());
  }
  
  
}
