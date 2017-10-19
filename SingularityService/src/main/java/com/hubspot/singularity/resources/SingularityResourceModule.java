package com.hubspot.singularity.resources;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.hubspot.singularity.SingularityServiceBaseModule;
import com.hubspot.singularity.config.UIConfiguration;
import com.hubspot.singularity.guice.GuicePropertyFilteringMessageBodyWriter;

public class SingularityResourceModule extends AbstractModule {
  private final UIConfiguration uiConfiguration;

  public SingularityResourceModule(UIConfiguration uiConfiguration) {
    this.uiConfiguration = checkNotNull(uiConfiguration, "uiConfiguration is null");
  }

  @Override
  protected void configure() {
    bind(GuicePropertyFilteringMessageBodyWriter.class).in(Scopes.SINGLETON);

    // At least WebhookResource must not be a singleton. Make all of them
    // not singletons, just in case.
    bind(DeployResource.class);
    bind(HistoryResource.class);
    bind(RackResource.class);
    bind(RequestResource.class);
    bind(S3LogResource.class);
    bind(SandboxResource.class);
    bind(SlaveResource.class);
    bind(StateResource.class);
    bind(TaskResource.class);
    bind(TestResource.class);
    bind(WebhookResource.class);
    bind(AuthResource.class);
    bind(MetricsResource.class);
    bind(UserResource.class);
    bind(DisastersResource.class);
    bind(PriorityResource.class);
    bind(UsageResource.class);
    bind(RequestGroupResource.class);
    bind(InactiveSlaveResource.class);
    bind(TaskTrackerResource.class);

    install(new SingularityServiceBaseModule(uiConfiguration));
  }
}
