package com.hubspot.singularity.data;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.usage.UsageManager;
import com.hubspot.singularity.helpers.RequestHelper;

public class SingularityDataModule extends AbstractModule {

  private final SingularityConfiguration configuration;

  public SingularityDataModule(final SingularityConfiguration configuration) {
    this.configuration = configuration;
  }

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
    bind(WebhookManager.class).in(Scopes.SINGLETON);
    bind(NotificationsManager.class).in(Scopes.SINGLETON);

    bind(ExecutorIdGenerator.class).asEagerSingleton();
    bind(WebhookManager.class).in(Scopes.SINGLETON);
    bind(DisasterManager.class).in(Scopes.SINGLETON);
    bind(PriorityManager.class).in(Scopes.SINGLETON);
    bind(RequestGroupManager.class).in(Scopes.SINGLETON);
    bind(AuthTokenManager.class).in(Scopes.SINGLETON);
  }
}
