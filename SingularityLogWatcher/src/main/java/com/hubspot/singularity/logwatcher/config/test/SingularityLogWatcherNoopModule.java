package com.hubspot.singularity.logwatcher.config.test;

import com.google.inject.AbstractModule;
import com.hubspot.singularity.logwatcher.LogForwarder;
import com.hubspot.singularity.logwatcher.SimpleStore;
import com.hubspot.singularity.logwatcher.impl.FileBasedSimpleStore;

public class SingularityLogWatcherNoopModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(SimpleStore.class).to(FileBasedSimpleStore.class);
    bind(LogForwarder.class).toInstance(new NoopLogForwarder());
  }

}
