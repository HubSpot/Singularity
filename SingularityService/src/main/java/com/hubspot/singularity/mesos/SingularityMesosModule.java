package com.hubspot.singularity.mesos;

import java.util.Collections;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.singularity.SingularityTaskRequest;

public class SingularityMesosModule extends AbstractModule {

  public static final String SCHEDULER_LOCK_NAME = "scheduler-lock";

  @Override
  public void configure() {
    bind(SingularityDriver.class).in(Scopes.SINGLETON);
    bind(SingularityLogSupport.class).in(Scopes.SINGLETON);
    bind(SingularityMesosScheduler.class).in(Scopes.SINGLETON);
    bind(SingularityMesosFrameworkMessageHandler.class).in(Scopes.SINGLETON);
    bind(SingularityMesosSchedulerDelegator.class).in(Scopes.SINGLETON);
    bind(SingularityMesosTaskBuilder.class).in(Scopes.SINGLETON);
    bind(SingularitySlaveAndRackManager.class).in(Scopes.SINGLETON);
    bind(SingularitySlaveAndRackHelper.class).in(Scopes.SINGLETON);
    bind(SingularityStartup.class).in(Scopes.SINGLETON);
    bind(SchedulerDriverSupplier.class).in(Scopes.SINGLETON);
  }

  @Provides
  @Named(SCHEDULER_LOCK_NAME)
  @Singleton
  public Lock getSchedulerLock() {
    return new ReentrantLock();
  }
}
