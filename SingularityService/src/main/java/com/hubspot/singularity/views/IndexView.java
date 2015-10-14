package com.hubspot.singularity.views;

import static com.google.common.base.Preconditions.checkNotNull;

import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.config.SingularityConfiguration;

import io.dropwizard.views.View;

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

  private final long defaultHealthcheckIntervalSeconds;
  private final long defaultHealthcheckTimeoutSeconds;
  private final long defaultDeployHealthTimeoutSeconds;

  private final String runningTaskLogPath;
  private final String finishedTaskLogPath;

  private final String commonHostnameSuffixToOmit;

  private final String taskS3LogOmitPrefix;

  private final Integer warnIfScheduledJobIsRunningPastNextRunPct;

  public IndexView(String singularityUriBase, String appRoot, SingularityConfiguration configuration) {
    super("index.mustache");

    checkNotNull(singularityUriBase, "singularityUriBase is null");

    String rawAppRoot = String.format("%s%s", singularityUriBase, appRoot);

    this.appRoot = (rawAppRoot.endsWith("/")) ? rawAppRoot.substring(0, rawAppRoot.length() - 1) : rawAppRoot;
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

    this.defaultHealthcheckIntervalSeconds = configuration.getHealthcheckIntervalSeconds();
    this.defaultHealthcheckTimeoutSeconds = configuration.getHealthcheckTimeoutSeconds();
    this.defaultDeployHealthTimeoutSeconds = configuration.getDeployHealthyBySeconds();

    this.runningTaskLogPath = configuration.getUiConfiguration().getRunningTaskLogPath();
    this.finishedTaskLogPath = configuration.getUiConfiguration().getFinishedTaskLogPath();

    this.commonHostnameSuffixToOmit = configuration.getCommonHostnameSuffixToOmit().or("");

    this.taskS3LogOmitPrefix = configuration.getUiConfiguration().getTaskS3LogOmitPrefix();

    this.warnIfScheduledJobIsRunningPastNextRunPct = configuration.getWarnIfScheduledJobIsRunningPastNextRunPct();
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

  public long getDefaultHealthcheckIntervalSeconds() {
    return defaultHealthcheckIntervalSeconds;
  }

  public long getDefaultHealthcheckTimeoutSeconds() {
    return defaultHealthcheckTimeoutSeconds;
  }

  public long getDefaultDeployHealthTimeoutSeconds() {
    return defaultDeployHealthTimeoutSeconds;
  }

  public String getRunningTaskLogPath() {
    return runningTaskLogPath;
  }

  public String getFinishedTaskLogPath() {
    return finishedTaskLogPath;
  }

  public String getCommonHostnameSuffixToOmit() {
    return commonHostnameSuffixToOmit;
  }

  public String getTaskS3LogOmitPrefix() {
    return taskS3LogOmitPrefix;
  }

  public Integer getWarnIfScheduledJobIsRunningPastNextRunPct() {
    return warnIfScheduledJobIsRunningPastNextRunPct;
  }

  @Override
  public String toString() {
    return "IndexView[" +
            "appRoot='" + appRoot + '\'' +
            ", apiDocs='" + apiDocs + '\'' +
            ", staticRoot='" + staticRoot + '\'' +
            ", apiRoot='" + apiRoot + '\'' +
            ", navColor='" + navColor + '\'' +
            ", defaultMemory=" + defaultMemory +
            ", defaultCpus=" + defaultCpus +
            ", hideNewDeployButton=" + hideNewDeployButton +
            ", hideNewRequestButton=" + hideNewRequestButton +
            ", title='" + title + '\'' +
            ", slaveHttpPort=" + slaveHttpPort +
            ", slaveHttpsPort=" + slaveHttpsPort +
            ", defaultHealthcheckIntervalSeconds=" + defaultHealthcheckIntervalSeconds +
            ", defaultHealthcheckTimeoutSeconds=" + defaultHealthcheckTimeoutSeconds +
            ", defaultDeployHealthTimeoutSeconds=" + defaultDeployHealthTimeoutSeconds +
            ", runningTaskLogPath='" + runningTaskLogPath + '\'' +
            ", finishedTaskLogPath='" + finishedTaskLogPath + '\'' +
            ", commonHostnameSuffixToOmit='" + commonHostnameSuffixToOmit + '\'' +
            ", warnIfScheduledJobIsRunningPastNextRunPct='" + warnIfScheduledJobIsRunningPastNextRunPct + '\'' +
            ']';
  }
}
