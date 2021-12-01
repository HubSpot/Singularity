package com.hubspot.singularity.mesos;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.hubspot.singularity.helpers.MesosProtosUtils;
import com.hubspot.singularity.helpers.TaskLagGuardrail;
import com.hubspot.singularity.hooks.DeployAcceptanceHook;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.mesos.v1.Protos.TaskStatus.Reason;

public class SingularityMesosModule extends AbstractModule {
  public static final String TASK_LOST_REASONS_COUNTER = "task-lost-reasons";
  public static final String ACTIVE_AGENTS_LOST_COUNTER = "active-agents-lost";
  public static final String FAILED_MESOS_CALLS = "failed-mesos-calls";

  @Override
  public void configure() {
    bind(MesosProtosUtils.class).in(Scopes.SINGLETON);
    bind(SingularityMesosExecutorInfoSupport.class).in(Scopes.SINGLETON);
    bind(SingularityMesosScheduler.class)
      .to(SingularityMesosSchedulerImpl.class)
      .in(Scopes.SINGLETON);
    bind(SingularityMesosFrameworkMessageHandler.class).in(Scopes.SINGLETON);
    bind(SingularityMesosTaskBuilder.class).in(Scopes.SINGLETON);
    bind(SingularityTaskSizeOptimizer.class).in(Scopes.SINGLETON);
    bind(SingularityAgentAndRackManager.class).in(Scopes.SINGLETON);
    bind(SingularityAgentAndRackHelper.class).in(Scopes.SINGLETON);
    bind(SingularityStartup.class).in(Scopes.SINGLETON);
    bind(SingularitySchedulerLock.class).in(Scopes.SINGLETON);
    bind(SingularityMesosSchedulerClient.class).in(Scopes.SINGLETON);
    bind(TaskLagGuardrail.class).in(Scopes.SINGLETON);

    Multibinder.newSetBinder(binder(), DeployAcceptanceHook.class);
  }

  @Provides
  @Named(TASK_LOST_REASONS_COUNTER)
  @Singleton
  public Multiset<Reason> provideTaskLostReasonsCounter() {
    return HashMultiset.create(Reason.getDescriptor().getValues().size());
  }

  @Provides
  @Named(ACTIVE_AGENTS_LOST_COUNTER)
  @Singleton
  public AtomicInteger provideActiveSlavesLostCounter() {
    return new AtomicInteger();
  }

  @Provides
  @Named(FAILED_MESOS_CALLS)
  @Singleton
  public AtomicLong provideFailedMesosCallsCounter() {
    return new AtomicLong();
  }
}
