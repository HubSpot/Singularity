package com.hubspot.singularity.logwatcher.config.test;

import com.google.inject.AbstractModule;
import com.hubspot.singularity.logwatcher.LogForwarder;
import com.hubspot.singularity.logwatcher.SimpleStore;

public class SingularityLogWatcherTestModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(SimpleStore.class).toInstance(new MemoryStore());
    bind(LogForwarder.class).toInstance(new LogLogForwarder());
  }
  
  
}
