package com.hubspot.singularity.mesos;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class SingularityMesosModule extends AbstractModule {
  @Override
  public void configure() {
    bind(SingularityDriver.class).in(Scopes.SINGLETON);
    bind(SingularityLogSupport.class).in(Scopes.SINGLETON);
    bind(SingularityMesosScheduler.class).in(Scopes.SINGLETON);
    bind(SingularityMesosSchedulerDelegator.class).in(Scopes.SINGLETON);
    bind(SingularityMesosTaskBuilder.class).in(Scopes.SINGLETON);
    bind(SingularitySlaveAndRackManager.class).in(Scopes.SINGLETON);
    bind(SingularityStartup.class).in(Scopes.SINGLETON);
    bind(SchedulerDriverSupplier.class).in(Scopes.SINGLETON);
  }
}
