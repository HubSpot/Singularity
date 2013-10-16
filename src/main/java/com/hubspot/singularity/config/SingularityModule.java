package com.hubspot.singularity.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.singularity.SingularityConfiguration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SingularityModule extends AbstractModule {
  @Override
  protected void configure() {
  }

  @Provides
  @Named("singularity.master")
  public String providesMaster(SingularityConfiguration config) {
    return config.getMaster();
  }

  @Provides
  @Singleton
  public ExecutorService providesExecutorService() {
    return Executors.newCachedThreadPool();
  }
}
