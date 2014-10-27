package com.hubspot.singularity.resources;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.hubspot.singularity.guice.GuicePropertyFilteringMessageBodyWriter;

public class SingularityResourceModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(GuicePropertyFilteringMessageBodyWriter.class).in(Scopes.SINGLETON);

    // At least WebhookResource must not be a singleton. Make all of them
    // not singletons, just in case.
    bind(DeployResource.class);
    bind(HistoryResource.class);
    bind(IndexResource.class);
    bind(RackResource.class);
    bind(RequestResource.class);
    bind(S3LogResource.class);
    bind(SandboxResource.class);
    bind(SlaveResource.class);
    bind(StateResource.class);
    bind(TaskResource.class);
    bind(TestResource.class);
    bind(WebhookResource.class);
  }
}
