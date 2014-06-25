package com.hubspot.singularity.views;

import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.auth.SingularityUser;
import com.hubspot.singularity.config.SingularityConfiguration;
import io.dropwizard.server.SimpleServerFactory;
import io.dropwizard.views.View;

public class IndexView extends View {
  private final String appRoot;
  private final String staticRoot;
  private final String apiRoot;

  private final Integer mesosLogsPort;
  private final Integer mesosLogsPortHttps;

  private final SingularityUser user;

  public IndexView(SingularityConfiguration configuration, SingularityUser user) {
    super("index.mustache");

    appRoot = configuration.getSingularityUIHostnameAndPath().or(((SimpleServerFactory)configuration.getServerFactory()).getApplicationContextPath());
    staticRoot = String.format("%s/static", appRoot);
    apiRoot = String.format("%s%s", appRoot, SingularityService.API_BASE_PATH);

    mesosLogsPort = configuration.getMesosConfiguration().getSlaveHttpPort();
    mesosLogsPortHttps = configuration.getMesosConfiguration().getSlaveHttpsPort().orNull();

    this.user = user;
  }

  public String getAppRoot() {
    return appRoot;
  }

  public String getStaticRoot() {
    return staticRoot;
  }

  public String getApiRoot() {
    return apiRoot;
  }

  public Integer getMesosLogsPort() {
    return mesosLogsPort;
  }

  public Integer getMesosLogsPortHttps() {
    return mesosLogsPortHttps;
  }

  public SingularityUser getUser() {
    return user;
  }
}
