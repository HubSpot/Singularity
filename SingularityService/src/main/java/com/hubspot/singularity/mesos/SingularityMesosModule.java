package com.hubspot.singularity.mesos;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mesos.v1.Protos.TaskStatus.Reason;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

public class SingularityMesosModule extends AbstractModule {

  public static final String TASK_LOST_REASONS_COUNTER = "task-lost-reasons";
  public static final String ACTIVE_SLAVES_LOST_COUNTER = "active-slaves-lost";

  @Override
  public void configure() {
    bind(SingularityMesosExecutorInfoSupport.class).in(Scopes.SINGLETON);
    bind(SingularityMesosScheduler.class).to(SingularityMesosSchedulerImpl.class).in(Scopes.SINGLETON);
    bind(SingularityMesosFrameworkMessageHandler.class).in(Scopes.SINGLETON);
    bind(SingularityMesosTaskBuilder.class).in(Scopes.SINGLETON);
    bind(SingularityTaskSizeOptimizer.class).in(Scopes.SINGLETON);
    bind(SingularitySlaveAndRackManager.class).in(Scopes.SINGLETON);
    bind(SingularitySlaveAndRackHelper.class).in(Scopes.SINGLETON);
    bind(SingularityStartup.class).in(Scopes.SINGLETON);
    bind(SingularitySchedulerLock.class).in(Scopes.SINGLETON);
    bind(SingularityMesosSchedulerClient.class).in(Scopes.SINGLETON);
  }

  @Provides
  @Named(TASK_LOST_REASONS_COUNTER)
  @Singleton
  public Multiset<Reason> provideTaskLostReasonsCounter() {
    return HashMultiset.create(Reason.getDescriptor().getValues().size());
  }

  @Provides
  @Named(ACTIVE_SLAVES_LOST_COUNTER)
  @Singleton
  public AtomicInteger provideActiveSlavesLostCounter() {
    return new AtomicInteger();
  }
}
