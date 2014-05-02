package com.hubspot.singularity.logwatcher.impl;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.hubspot.singularity.logwatcher.LogForwarder;
import com.hubspot.singularity.logwatcher.SimpleStore;

public class SingularityLogWatcherImplModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(SimpleStore.class).to(FileBasedSimpleStore.class).in(Scopes.SINGLETON);
    bind(LogForwarder.class).to(FluentdLogForwarder.class).in(Scopes.SINGLETON);
  }

}
