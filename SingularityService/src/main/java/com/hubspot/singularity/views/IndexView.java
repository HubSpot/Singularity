package com.hubspot.singularity.views;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import io.dropwizard.server.SimpleServerFactory;
import io.dropwizard.views.View;

import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.config.SingularityConfiguration;

public class IndexView extends View {

  private final String appRoot;
  private final String staticRoot;
  private final String apiRoot;
  private final String navColor;

  private final Integer defaultMemory;
  private final Integer defaultCpus;

  private final Boolean hideNewDeployButton;
  private final Boolean hideNewRequestButton;

  private final String title;

  private final Integer slaveHttpPort;
  private final Integer slaveHttpsPort;

  private final String taskQuickLinks;

  public IndexView(SingularityConfiguration configuration, ObjectMapper objectMapper) {
    super("index.mustache");

    appRoot = configuration.getUiConfiguration().getBaseUrl().or(((SimpleServerFactory) configuration.getServerFactory()).getApplicationContextPath());
    staticRoot = String.format("%s/static", appRoot);
    apiRoot = String.format("%s%s", appRoot, SingularityService.API_BASE_PATH);

    title = configuration.getUiConfiguration().getTitle();

    slaveHttpPort = configuration.getMesosConfiguration().getSlaveHttpPort();
    slaveHttpsPort = configuration.getMesosConfiguration().getSlaveHttpsPort().orNull();

    defaultCpus = configuration.getMesosConfiguration().getDefaultCpus();
    defaultMemory = configuration.getMesosConfiguration().getDefaultMemory();

    hideNewDeployButton = configuration.getUiConfiguration().isHideNewDeployButton();
    hideNewRequestButton = configuration.getUiConfiguration().isHideNewRequestButton();

    navColor = configuration.getUiConfiguration().getNavColor();

    try {
      taskQuickLinks = objectMapper.writeValueAsString(configuration.getUiConfiguration().getTaskQuickLinks());
    }
    catch (JsonProcessingException e) {
      throw Throwables.propagate(e);
    }
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

  public String getTitle() {
    return title;
  }

  public String getNavColor() {
    return navColor;
  }

  public Integer getSlaveHttpPort() {
    return slaveHttpPort;
  }

  public Integer getSlaveHttpsPort() {
    return slaveHttpsPort;
  }

  public Integer getDefaultMemory() {
    return defaultMemory;
  }

  public Integer getDefaultCpus() {
    return defaultCpus;
  }

  public Boolean getHideNewDeployButton() {
    return hideNewDeployButton;
  }

  public Boolean getHideNewRequestButton() {
    return hideNewRequestButton;
  }

  public String getTaskQuickLinks() {
    return taskQuickLinks;
  }


  @Override
  public String toString() {
    return "IndexView [appRoot=" + appRoot + ", staticRoot=" + staticRoot + ", apiRoot=" + apiRoot + ", navColor=" + navColor + ", defaultMemory=" + defaultMemory + ", defaultCpus=" + defaultCpus
        + ", hideNewDeployButton=" + hideNewDeployButton + ", hideNewRequestButton=" + hideNewRequestButton + ", title=" + title + ", slaveHttpPort=" + slaveHttpPort + ", slaveHttpsPort="
        + slaveHttpsPort + ", taskQuickLinks=" + taskQuickLinks + "]";
  }

}
