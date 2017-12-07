package com.hubspot.singularity.views;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Throwables;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.IndexViewConfiguration;
import com.hubspot.singularity.config.UIConfiguration;

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
  private final Boolean loadBalancingEnabled;

  private final String title;

  private final Integer slaveHttpPort;
  private final Integer slaveHttpsPort;

  private final int defaultBounceExpirationMinutes;
  private final long defaultHealthcheckIntervalSeconds;
  private final long defaultHealthcheckTimeoutSeconds;
  private final Integer defaultHealthcheckMaxRetries;
  private final int defaultStartupTimeoutSeconds;

  private final String runningTaskLogPath;
  private final String finishedTaskLogPath;

  private final String commonHostnameSuffixToOmit;

  private final String taskS3LogOmitPrefix;

  private final Integer warnIfScheduledJobIsRunningPastNextRunPct;

  private final String shellCommands;

  private final boolean shortenSlaveUsageHostname;

  private final String timestampFormat;

  private final boolean showTaskDiskResource;

  private final String timestampWithSecondsFormat;

  private final String redirectOnUnauthorizedUrl;

  private final String extraScript;

  private final boolean generateAuthHeader;
  private final String authCookieName;
  private final String authTokenKey;
  private final String quickLinks;

  public IndexView(String singularityUriBase, String appRoot, IndexViewConfiguration configuration, ObjectMapper mapper) {
    super("index.mustache");

    checkNotNull(singularityUriBase, "singularityUriBase is null");

    UIConfiguration uiConfiguration = configuration.getUiConfiguration();

    String rawAppRoot = String.format("%s%s", singularityUriBase, appRoot);

    this.appRoot = uiConfiguration.getAppRootOverride().or((rawAppRoot.endsWith("/")) ? rawAppRoot.substring(0, rawAppRoot.length() - 1) : rawAppRoot);
    this.staticRoot = uiConfiguration.getStaticRootOverride().or(String.format("%s/static", singularityUriBase));
    this.apiDocs = String.format("%s/api-docs/", singularityUriBase);
    this.apiRoot = uiConfiguration.getApiRootOverride().or(String.format("%s%s", singularityUriBase, ApiPaths.API_BASE_PATH));

    this.title = uiConfiguration.getTitle();

    this.slaveHttpPort = configuration.getSlaveHttpPort();
    this.slaveHttpsPort = configuration.getSlaveHttpsPort().orNull();

    this.defaultCpus = configuration.getDefaultCpus();
    this.defaultMemory = configuration.getDefaultMemory();

    this.hideNewDeployButton = uiConfiguration.isHideNewDeployButton();
    this.hideNewRequestButton = uiConfiguration.isHideNewRequestButton();
    this.loadBalancingEnabled = configuration.isLoadBalancingEnabled();

    this.navColor = uiConfiguration.getNavColor().or("");

    this.defaultBounceExpirationMinutes = configuration.getBounceExpirationMinutes();
    this.defaultHealthcheckIntervalSeconds = configuration.getHealthcheckIntervalSeconds();
    this.defaultHealthcheckTimeoutSeconds = configuration.getHealthcheckTimeoutSeconds();
    this.defaultHealthcheckMaxRetries = configuration.getHealthcheckMaxRetries().or(0);
    this.defaultStartupTimeoutSeconds = configuration.getStartupTimeoutSeconds();

    this.runningTaskLogPath = uiConfiguration.getRunningTaskLogPath();
    this.finishedTaskLogPath = uiConfiguration.getFinishedTaskLogPath();

    this.showTaskDiskResource = uiConfiguration.isShowTaskDiskResource();

    this.commonHostnameSuffixToOmit = configuration.getCommonHostnameSuffixToOmit().or("");

    this.taskS3LogOmitPrefix = uiConfiguration.getTaskS3LogOmitPrefix().or("");

    this.warnIfScheduledJobIsRunningPastNextRunPct = configuration.getWarnIfScheduledJobIsRunningPastNextRunPct();

    this.redirectOnUnauthorizedUrl = uiConfiguration.getRedirectOnUnauthorizedUrl().or("");

    ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
    try {
      this.shellCommands = ow.writeValueAsString(uiConfiguration.getShellCommands());
    } catch (JsonProcessingException e) {
      throw Throwables.propagate(e);
    }

    this.shortenSlaveUsageHostname = uiConfiguration.isShortenSlaveUsageHostname();

    this.timestampFormat = uiConfiguration.getTimestampFormat();

    this.timestampWithSecondsFormat = uiConfiguration.getTimestampWithSecondsFormat();

    this.extraScript = uiConfiguration.getExtraScript().orNull();

    this.generateAuthHeader = configuration.isGenerateAuthHeader();
    this.authCookieName = uiConfiguration.getAuthCookieName();
    this.authTokenKey = uiConfiguration.getAuthTokenKey();

    try {
      this.quickLinks = ow.writeValueAsString(uiConfiguration.getQuickLinks());
    } catch (JsonProcessingException e) {
      throw Throwables.propagate(e);
    }
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

  public Boolean getLoadBalancingEnabled() {
    return loadBalancingEnabled;
  }

  public int getDefaultBounceExpirationMinutes() {
    return defaultBounceExpirationMinutes;
  }

  public long getDefaultHealthcheckIntervalSeconds() {
    return defaultHealthcheckIntervalSeconds;
  }

  public long getDefaultHealthcheckTimeoutSeconds() {
    return defaultHealthcheckTimeoutSeconds;
  }

  public Integer getDefaultHealthcheckMaxRetries() {
    return defaultHealthcheckMaxRetries;
  }

  public int getDefaultStartupTimeoutSeconds() {
    return defaultStartupTimeoutSeconds;
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

  public Boolean isShowTaskDiskResource() {
    return showTaskDiskResource;
  }

  public String getTaskS3LogOmitPrefix() {
    return taskS3LogOmitPrefix;
  }

  public Integer getWarnIfScheduledJobIsRunningPastNextRunPct() {
    return warnIfScheduledJobIsRunningPastNextRunPct;
  }

  public String getShellCommands() {
    return shellCommands;
  }

  public String getTimestampFormat() {
    return timestampFormat;
  }

  public String getTimestampWithSecondsFormat() {
    return timestampWithSecondsFormat;
  }

  public String getExtraScript() {
    return extraScript;
  }

  public String getRedirectOnUnauthorizedUrl() {
    return redirectOnUnauthorizedUrl;
  }

  public boolean isShortenSlaveUsageHostname() {
    return shortenSlaveUsageHostname;
  }

  public boolean isGenerateAuthHeader() {
    return generateAuthHeader;
  }

  public String getAuthCookieName() {
    return authCookieName;
  }

  public String getAuthTokenKey() {
    return authTokenKey;
  }

  public String getQuickLinks() {
    return quickLinks;
  }

  @Override
  public String toString() {
    return "IndexView{" +
        "appRoot='" + appRoot + '\'' +
        ", apiDocs='" + apiDocs + '\'' +
        ", staticRoot='" + staticRoot + '\'' +
        ", apiRoot='" + apiRoot + '\'' +
        ", navColor='" + navColor + '\'' +
        ", defaultMemory=" + defaultMemory +
        ", defaultCpus=" + defaultCpus +
        ", hideNewDeployButton=" + hideNewDeployButton +
        ", hideNewRequestButton=" + hideNewRequestButton +
        ", loadBalancingEnabled=" + loadBalancingEnabled +
        ", title='" + title + '\'' +
        ", slaveHttpPort=" + slaveHttpPort +
        ", slaveHttpsPort=" + slaveHttpsPort +
        ", defaultBounceExpirationMinutes=" + defaultBounceExpirationMinutes +
        ", defaultHealthcheckIntervalSeconds=" + defaultHealthcheckIntervalSeconds +
        ", defaultHealthcheckTimeoutSeconds=" + defaultHealthcheckTimeoutSeconds +
        ", defaultHealthcheckMaxRetries=" + defaultHealthcheckMaxRetries +
        ", defaultStartupTimeoutSeconds=" + defaultStartupTimeoutSeconds +
        ", runningTaskLogPath='" + runningTaskLogPath + '\'' +
        ", finishedTaskLogPath='" + finishedTaskLogPath + '\'' +
        ", commonHostnameSuffixToOmit='" + commonHostnameSuffixToOmit + '\'' +
        ", taskS3LogOmitPrefix='" + taskS3LogOmitPrefix + '\'' +
        ", warnIfScheduledJobIsRunningPastNextRunPct=" + warnIfScheduledJobIsRunningPastNextRunPct +
        ", shellCommands='" + shellCommands + '\'' +
        ", shortenSlaveUsageHostname=" + shortenSlaveUsageHostname +
        ", timestampFormat='" + timestampFormat + '\'' +
        ", showTaskDiskResource=" + showTaskDiskResource +
        ", timestampWithSecondsFormat='" + timestampWithSecondsFormat + '\'' +
        ", redirectOnUnauthorizedUrl='" + redirectOnUnauthorizedUrl + '\'' +
        ", extraScript='" + extraScript + '\'' +
        ", generateAuthHeader=" + generateAuthHeader +
        ", authCookieName='" + authCookieName + '\'' +
        ", authTokenKey='" + authTokenKey + '\'' +
        ", quickLinks='" + quickLinks + '\'' +
        "} " + super.toString();
  }
}
