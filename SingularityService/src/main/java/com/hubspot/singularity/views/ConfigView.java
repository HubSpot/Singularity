package com.hubspot.singularity.views;

import com.hubspot.singularity.config.SingularityConfiguration;
import io.dropwizard.server.SimpleServerFactory;
import io.dropwizard.views.View;

public class ConfigView extends View {
  private final String appRoot;
  private final String apiBase;
  private final Integer mesosLogsPort;
  private final Integer mesosLogsPortHttps;

  public ConfigView(SingularityConfiguration configuration) {
    super("config.mustache");

    appRoot = configuration.getSingularityUIHostnameAndPath().or(((SimpleServerFactory)configuration.getServerFactory()).getApplicationContextPath());
    apiBase = String.format("%s/v1", appRoot);
    mesosLogsPort = configuration.getMesosConfiguration().getSlaveHttpPort();
    mesosLogsPortHttps = configuration.getMesosConfiguration().getSlaveHttpsPort().orNull();
  }

  public String getAppRoot() {
    return appRoot;
  }

  public String getApiBase() {
    return apiBase;
  }

  public Integer getMesosLogsPort() {
    return mesosLogsPort;
  }

  public Integer getMesosLogsPortHttps() {
    return mesosLogsPortHttps;
  }
}
