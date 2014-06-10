package com.hubspot.singularity.views;

import io.dropwizard.server.SimpleServerFactory;
import io.dropwizard.views.View;

import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.config.SingularityConfiguration;

public class IndexView extends View {
  private final String appRoot;
  private final String staticRoot;
  private final String apiRoot;

  private final Integer mesosLogsPort;
  private final Integer mesosLogsPortHttps;

  public IndexView(SingularityConfiguration configuration) {
    super("index.mustache");

    appRoot = configuration.getSingularityUIHostnameAndPath().or(((SimpleServerFactory)configuration.getServerFactory()).getApplicationContextPath());
    staticRoot = String.format("%s/static", appRoot);
    apiRoot = String.format("%s%s", appRoot, SingularityService.API_BASE_PATH);

    mesosLogsPort = configuration.getMesosConfiguration().getSlaveHttpPort();
    mesosLogsPortHttps = configuration.getMesosConfiguration().getSlaveHttpsPort().orNull();
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
}
