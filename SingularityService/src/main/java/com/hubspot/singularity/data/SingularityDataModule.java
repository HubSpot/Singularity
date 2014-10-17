package com.hubspot.singularity.data;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

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
}
