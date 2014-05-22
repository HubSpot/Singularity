package com.hubspot.singularity.views;

import com.hubspot.singularity.config.SingularityConfiguration;
import io.dropwizard.server.SimpleServerFactory;
import io.dropwizard.views.View;

public class IndexView extends View {
  private final String staticPath;
  private final String basePath;

  public IndexView(SingularityConfiguration configuration) {
    super("index.mustache");

    basePath = configuration.getSingularityUIHostnameAndPath().or(((SimpleServerFactory)configuration.getServerFactory()).getApplicationContextPath());
    staticPath = String.format("%s/static", basePath);
  }

  public String getStaticPath() {
    return staticPath;
  }

  public String getBasePath() {
    return basePath;
  }
}
