package com.hubspot.singularity.resources;

import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.hubspot.singularity.config.UIConfiguration;
import com.hubspot.singularity.guice.GuicePropertyFilteringMessageBodyWriter;

public class SingularityResourceModule extends AbstractModule {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityResourceModule.class);

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

    switch (uiConfiguration.getRootUrlMode()) {
    case UI_REDIRECT: {
      bind(UiResource.class);
      bind(IndexResource.class);
      break;
    }
    case INDEX_CATCHALL: {
      bind(StaticCatchallResource.class);
      break;
    }
    case DISABLED:
    default: {
      bind(UiResource.class);
      LOG.info("No resources bound to / or /*");
      break;
    }
    }
  }
}
