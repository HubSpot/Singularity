package com.hubspot.singularity.views;

import static com.google.common.base.Preconditions.checkNotNull;

import io.dropwizard.views.View;

import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.config.SingularityConfiguration;

public class IndexView extends View {

  private final String appRoot;
  private final String apiDocs;
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

  public IndexView(String singularityUriBase, String appRoot, SingularityConfiguration configuration) {
    super("index.mustache");

    checkNotNull(singularityUriBase, "singularityUriBase is null");

    this.appRoot = String.format("%s%s", singularityUriBase, appRoot);
    this.staticRoot = String.format("%s/static", singularityUriBase);
    this.apiDocs = String.format("%s/api-docs", singularityUriBase);
    this.apiRoot = String.format("%s%s", singularityUriBase, SingularityService.API_BASE_PATH);

    this.title = configuration.getUiConfiguration().getTitle();

    this.slaveHttpPort = configuration.getMesosConfiguration().getSlaveHttpPort();
    this.slaveHttpsPort = configuration.getMesosConfiguration().getSlaveHttpsPort().orNull();

    this.defaultCpus = configuration.getMesosConfiguration().getDefaultCpus();
    this.defaultMemory = configuration.getMesosConfiguration().getDefaultMemory();

    this.hideNewDeployButton = configuration.getUiConfiguration().isHideNewDeployButton();
    this.hideNewRequestButton = configuration.getUiConfiguration().isHideNewRequestButton();

    this.navColor = configuration.getUiConfiguration().getNavColor();
  }

  public String getAppRoot() {
    return appRoot;
  }

  public String getStaticRoot() {
    return staticRoot;
  }

  public String getApiDocs() {
    return apiDocs;
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

  @Override
  public String toString() {
    return "IndexView [appRoot=" + appRoot + ", staticRoot=" + staticRoot + ", apiRoot=" + apiRoot + ", navColor=" + navColor + ", defaultMemory=" + defaultMemory + ", defaultCpus=" + defaultCpus
        + ", hideNewDeployButton=" + hideNewDeployButton + ", hideNewRequestButton=" + hideNewRequestButton + ", title=" + title + ", slaveHttpPort=" + slaveHttpPort + ", slaveHttpsPort="
        + slaveHttpsPort + "]";
  }

}
