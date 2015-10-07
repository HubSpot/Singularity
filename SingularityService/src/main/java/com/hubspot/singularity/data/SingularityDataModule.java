package com.hubspot.singularity.data;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.config.SingularityConfiguration;

public class SingularityDataModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(MetadataManager.class).in(Scopes.SINGLETON);
    bind(StateManager.class).in(Scopes.SINGLETON);
    bind(TaskManager.class).in(Scopes.SINGLETON);
    bind(DeployManager.class).in(Scopes.SINGLETON);
    bind(RackManager.class).in(Scopes.SINGLETON);
    bind(RequestManager.class).in(Scopes.SINGLETON);
    bind(SlaveManager.class).in(Scopes.SINGLETON);
    bind(TaskRequestManager.class).in(Scopes.SINGLETON);
    bind(SandboxManager.class).in(Scopes.SINGLETON);
    bind(SingularityValidator.class).in(Scopes.SINGLETON);

    bind(ExecutorIdGenerator.class).in(Scopes.SINGLETON);
    bind(WebhookManager.class).in(Scopes.SINGLETON);
  }

  @Provides
  @Singleton
  public ZkCache<SingularityTask> taskCache(SingularityConfiguration configuration, MetricRegistry registry) {
    return new ZkCache<SingularityTask>(configuration.getCacheTasksMaxSize(), configuration.getCacheTasksInitialSize(), registry, "tasks");
  }

}
