package com.hubspot.singularity.executor.cleanup.config;

import com.google.common.base.Joiner;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.singularity.client.SingularityClientModule;

public class SingularityExecutorCleanupModule extends AbstractModule {
  @Override
  protected void configure() {

  }

  @Provides
  @Singleton
  @Named(SingularityClientModule.HOSTS_PROPERTY_NAME)
  public String providesSingularityHosts(SingularityExecutorCleanupConfiguration configuration) {
    return Joiner.on(",").join(configuration.getSingularityHosts());
  }

  @Provides
  @Singleton
  @Named(SingularityClientModule.CONTEXT_PATH)
  public String providesSingularityContextPath(SingularityExecutorCleanupConfiguration configuration) {
    return configuration.getSingularityContextPath();
  }
}
