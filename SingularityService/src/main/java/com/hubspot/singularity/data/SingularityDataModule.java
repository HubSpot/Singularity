package com.hubspot.singularity.data;

import org.apache.curator.framework.CuratorFramework;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.helpers.RequestHelper;

public class SingularityDataModule extends AbstractModule {

  public static final String REQUEST_ID_CACHE_NAME = "requestid.cache";
  public static final String TASK_CLEANUP_ID_CACHE_NAME = "taskcleanupid.cache";

  @Override
  protected void configure() {
    bind(RequestHelper.class).in(Scopes.SINGLETON);

    bind(MetadataManager.class).in(Scopes.SINGLETON);
    bind(StateManager.class).in(Scopes.SINGLETON);
    bind(TaskManager.class).in(Scopes.SINGLETON);
    bind(DeployManager.class).in(Scopes.SINGLETON);
    bind(RackManager.class).in(Scopes.SINGLETON);
    bind(RequestManager.class).in(Scopes.SINGLETON);
    bind(SlaveManager.class).in(Scopes.SINGLETON);
    bind(InactiveSlaveManager.class).in(Scopes.SINGLETON);
    bind(TaskRequestManager.class).in(Scopes.SINGLETON);
    bind(SandboxManager.class).in(Scopes.SINGLETON);
    bind(SingularityValidator.class).in(Scopes.SINGLETON);
    bind(UserManager.class).in(Scopes.SINGLETON);
    bind(UsageManager.class).in(Scopes.SINGLETON);
    bind(ExecutorIdGenerator.class).in(Scopes.SINGLETON);
    bind(WebhookManager.class).in(Scopes.SINGLETON);
    bind(DisasterManager.class).in(Scopes.SINGLETON);
    bind(PriorityManager.class).in(Scopes.SINGLETON);
    bind(RequestGroupManager.class).in(Scopes.SINGLETON);
  }

  @Provides
  @Singleton
  public ZkCache<SingularityTask> taskCache(SingularityConfiguration configuration, MetricRegistry registry) {
    return new ZkCache<>(configuration.getCacheTasksMaxSize(), configuration.getCacheTasksInitialSize(), configuration.getCacheTasksForMillis(), registry, "tasks");
  }

  @Provides
  @Singleton
  @Named(REQUEST_ID_CACHE_NAME)
  public ZkChildrenCache requestIdCache(CuratorFramework curator, MetricRegistry registry) {
    return new ZkChildrenCache(curator, "requestIds", registry);
  }

  @Provides
  @Singleton
  @Named(TASK_CLEANUP_ID_CACHE_NAME)
  public ZkChildrenCache cleanupIdCache(CuratorFramework curator, MetricRegistry registry) {
    return new ZkChildrenCache(curator, "taskCleanupIds", registry);
  }

}
