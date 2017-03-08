package com.hubspot.singularity.views;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
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

  public IndexView(String singularityUriBase, String appRoot, SingularityConfiguration configuration, ObjectMapper mapper) {
    super("index.mustache");

    checkNotNull(singularityUriBase, "singularityUriBase is null");

    String rawAppRoot = String.format("%s%s", singularityUriBase, appRoot);

    this.appRoot = (rawAppRoot.endsWith("/")) ? rawAppRoot.substring(0, rawAppRoot.length() - 1) : rawAppRoot;
    this.staticRoot = String.format("%s/static", singularityUriBase);
    this.apiDocs = String.format("%s/api-docs/", singularityUriBase);
    this.apiRoot = String.format("%s%s", singularityUriBase, SingularityService.API_BASE_PATH);

    this.title = configuration.getUiConfiguration().getTitle();

    this.slaveHttpPort = configuration.getMesosConfiguration().getSlaveHttpPort();
    this.slaveHttpsPort = configuration.getMesosConfiguration().getSlaveHttpsPort().orNull();

    this.defaultCpus = configuration.getMesosConfiguration().getDefaultCpus();
    this.defaultMemory = configuration.getMesosConfiguration().getDefaultMemory();

    this.hideNewDeployButton = configuration.getUiConfiguration().isHideNewDeployButton();
    this.hideNewRequestButton = configuration.getUiConfiguration().isHideNewRequestButton();
    this.loadBalancingEnabled = !Strings.isNullOrEmpty(configuration.getLoadBalancerUri());

    this.navColor = configuration.getUiConfiguration().getNavColor().or("");

    this.defaultBounceExpirationMinutes = configuration.getDefaultBounceExpirationMinutes();
    this.defaultHealthcheckIntervalSeconds = configuration.getHealthcheckIntervalSeconds();
    this.defaultHealthcheckTimeoutSeconds = configuration.getHealthcheckTimeoutSeconds();
    this.defaultHealthcheckMaxRetries = configuration.getHealthcheckMaxRetries().or(0);
    this.defaultStartupTimeoutSeconds = configuration.getStartupTimeoutSeconds();

    this.runningTaskLogPath = configuration.getUiConfiguration().getRunningTaskLogPath();
    this.finishedTaskLogPath = configuration.getUiConfiguration().getFinishedTaskLogPath();

    this.showTaskDiskResource = configuration.getUiConfiguration().isShowTaskDiskResource();

    this.commonHostnameSuffixToOmit = configuration.getCommonHostnameSuffixToOmit().or("");

    this.taskS3LogOmitPrefix = configuration.getUiConfiguration().getTaskS3LogOmitPrefix().or("");

    this.warnIfScheduledJobIsRunningPastNextRunPct = configuration.getWarnIfScheduledJobIsRunningPastNextRunPct();

    this.redirectOnUnauthorizedUrl = configuration.getUiConfiguration().getRedirectOnUnauthorizedUrl().or("");

    ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
    try {
      this.shellCommands = ow.writeValueAsString(configuration.getUiConfiguration().getShellCommands());
    } catch (JsonProcessingException e) {
      throw Throwables.propagate(e);
    }

    this.shortenSlaveUsageHostname = configuration.getUiConfiguration().isShortenSlaveUsageHostname();

    this.timestampFormat = configuration.getUiConfiguration().getTimestampFormat();

    this.timestampWithSecondsFormat = configuration.getUiConfiguration().getTimestampWithSecondsFormat();

    this.extraScript = configuration.getUiConfiguration().getExtraScript().orNull();
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
        "} " + super.toString();
  }
}
