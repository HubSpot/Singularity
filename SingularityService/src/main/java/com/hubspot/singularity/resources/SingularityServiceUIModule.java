package com.hubspot.singularity.resources;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.AbstractModule;
import com.hubspot.singularity.config.UIConfiguration;
import com.hubspot.singularity.resources.ui.IndexResource;
import com.hubspot.singularity.resources.ui.StaticCatchallResource;
import com.hubspot.singularity.resources.ui.UiResource;

public class SingularityServiceUIModule extends AbstractModule {
  public static final String SINGULARITY_URI_BASE = "_singularity_uri_base";

  private final UIConfiguration uiConfiguration;

  public SingularityServiceUIModule(UIConfiguration uiConfiguration) {
    this.uiConfiguration = checkNotNull(uiConfiguration, "uiConfiguration is null");
  }

  @Override
  public void configure() {
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
        break;
      }
    }
  }
}
