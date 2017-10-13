package com.hubspot.singularity.resources;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hubspot.singularity.SingularityMainModule.SINGULARITY_URI_BASE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.config.UIConfiguration;
import com.hubspot.singularity.guice.GuicePropertyFilteringMessageBodyWriter;
import com.hubspot.singularity.views.IndexView;

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

  @Provides
  @Singleton
  public IndexView providesIndexView(@Named(SINGULARITY_URI_BASE) String singularityUriBase, SingularityConfiguration configuration, ObjectMapper mapper) {
    return new IndexView(singularityUriBase, UiResource.UI_RESOURCE_LOCATION, configuration, mapper);
  }
}
