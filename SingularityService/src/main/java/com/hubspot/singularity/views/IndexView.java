package com.hubspot.singularity.views;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.io.Resources;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.IndexViewConfiguration;
import com.hubspot.singularity.config.UIConfiguration;
import io.dropwizard.views.View;
import java.io.IOException;
import java.util.Map;

public class IndexView extends View {
  private final String appRoot;
  private final String apiDocs;
  private final String staticRoot;
  private final String apiRoot;
  private final String navColor;

  private final Integer defaultMemory;
  private final Integer defaultCpus;
  private final Integer defaultDisk;

  private final Boolean hideNewDeployButton;
  private final Boolean hideNewRequestButton;
  private final Boolean loadBalancingEnabled;

  private final String title;

  private final Integer agentHttpPort;
  private final Integer agentHttpsPort;

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

  private final boolean shortenAgentUsageHostname;

  private final String timestampFormat;

  private final boolean showTaskDiskResource;

  private final String timestampWithSecondsFormat;

  private final String redirectOnUnauthorizedUrl;

  private final String extraScript;

  private final boolean generateAuthHeader;
  private final String authCookieName;
  private final String authTokenKey;
  private final String quickLinks;
  private final String navTitleLinks;
  private final String lessTerminalPath;
  private final String showRequestButtonsForGroup;
  private final String appJsPath;
  private final String appCssPath;
  private final String vendorJsPath;

  public IndexView(
    String singularityUriBase,
    String appRoot,
    IndexViewConfiguration configuration,
    ObjectMapper mapper
  ) {
    super("index.mustache");
    checkNotNull(singularityUriBase, "singularityUriBase is null");

    UIConfiguration uiConfiguration = configuration.getUiConfiguration();

    String rawAppRoot = String.format("%s%s", singularityUriBase, appRoot);

    this.appRoot =
      uiConfiguration
        .getAppRootOverride()
        .orElse(
          (rawAppRoot.endsWith("/"))
            ? rawAppRoot.substring(0, rawAppRoot.length() - 1)
            : rawAppRoot
        );
    this.staticRoot =
      uiConfiguration
        .getStaticRootOverride()
        .orElse(String.format("%s/static", singularityUriBase));
    this.apiDocs = String.format("%s/api-docs/", singularityUriBase);
    this.apiRoot =
      uiConfiguration
        .getApiRootOverride()
        .orElse(String.format("%s%s", singularityUriBase, ApiPaths.API_BASE_PATH));

    this.title = uiConfiguration.getTitle();

    this.agentHttpPort = configuration.getAgentHttpPort();
    this.agentHttpsPort = configuration.getAgentHttpsPort().orElse(null);

    this.defaultCpus = configuration.getDefaultCpus();
    this.defaultMemory = configuration.getDefaultMemory();
    this.defaultDisk = configuration.getDefaultDisk();

    this.hideNewDeployButton = uiConfiguration.isHideNewDeployButton();
    this.hideNewRequestButton = uiConfiguration.isHideNewRequestButton();
    this.loadBalancingEnabled = configuration.isLoadBalancingEnabled();

    this.navColor = uiConfiguration.getNavColor().orElse("");

    this.defaultBounceExpirationMinutes = configuration.getBounceExpirationMinutes();
    this.defaultHealthcheckIntervalSeconds =
      configuration.getHealthcheckIntervalSeconds();
    this.defaultHealthcheckTimeoutSeconds = configuration.getHealthcheckTimeoutSeconds();
    this.defaultHealthcheckMaxRetries =
      configuration.getHealthcheckMaxRetries().orElse(0);
    this.defaultStartupTimeoutSeconds = configuration.getStartupTimeoutSeconds();

    this.runningTaskLogPath = uiConfiguration.getRunningTaskLogPath();
    this.finishedTaskLogPath = uiConfiguration.getFinishedTaskLogPath();

    this.showTaskDiskResource = uiConfiguration.isShowTaskDiskResource();

    this.commonHostnameSuffixToOmit =
      configuration.getCommonHostnameSuffixToOmit().orElse("");

    this.taskS3LogOmitPrefix = uiConfiguration.getTaskS3LogOmitPrefix().orElse("");

    this.warnIfScheduledJobIsRunningPastNextRunPct =
      configuration.getWarnIfScheduledJobIsRunningPastNextRunPct();

    this.redirectOnUnauthorizedUrl =
      uiConfiguration.getRedirectOnUnauthorizedUrl().orElse("");

    ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
    try {
      this.shellCommands = ow.writeValueAsString(uiConfiguration.getShellCommands());
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    this.shortenAgentUsageHostname = uiConfiguration.isShortenAgentUsageHostname();

    this.timestampFormat = uiConfiguration.getTimestampFormat();

    this.timestampWithSecondsFormat = uiConfiguration.getTimestampWithSecondsFormat();

    this.extraScript = uiConfiguration.getExtraScript().orElse(null);

    this.generateAuthHeader = configuration.isGenerateAuthHeader();
    this.authCookieName = uiConfiguration.getAuthCookieName();
    this.authTokenKey = uiConfiguration.getAuthTokenKey();

    try {
      this.quickLinks = ow.writeValueAsString(uiConfiguration.getQuickLinks());
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    try {
      this.navTitleLinks = ow.writeValueAsString(uiConfiguration.getNavTitleLinks());
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    this.lessTerminalPath = uiConfiguration.getLessTerminalPath().orElse("");
    this.showRequestButtonsForGroup =
      uiConfiguration.getShowRequestButtonsForGroup().orElse("");

    try {
      Map<String, String> revManifest = mapper.readValue(
        Resources.getResource("assets/static/rev-manifest.json"),
        new TypeReference<Map<String, String>>() {}
      );
      this.appCssPath = revManifest.get("css/app.css");
      this.appJsPath = revManifest.get("js/app.bundle.js");
      this.vendorJsPath = revManifest.get("js/vendor.bundle.js");
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
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

  public Integer getAgentHttpPort() {
    return agentHttpPort;
  }

  public Integer getAgentHttpsPort() {
    return agentHttpsPort;
  }

  public Integer getDefaultMemory() {
    return defaultMemory;
  }

  public Integer getDefaultCpus() {
    return defaultCpus;
  }

  public Integer getDefaultDisk() {
    return defaultDisk;
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

  public boolean isShortenAgentUsageHostname() {
    return shortenAgentUsageHostname;
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

  public String getNavTitleLinks() {
    return navTitleLinks;
  }

  public String getLessTerminalPath() {
    return lessTerminalPath;
  }

  public String getAppJsPath() {
    return appJsPath;
  }

  public String getAppCssPath() {
    return appCssPath;
  }

  public String getVendorJsPath() {
    return vendorJsPath;
  }

  public String getShowRequestButtonsForGroup() {
    return showRequestButtonsForGroup;
  }

  @Override
  public String toString() {
    return (
      "IndexView{" +
      "appRoot='" +
      appRoot +
      '\'' +
      ", apiDocs='" +
      apiDocs +
      '\'' +
      ", staticRoot='" +
      staticRoot +
      '\'' +
      ", apiRoot='" +
      apiRoot +
      '\'' +
      ", navColor='" +
      navColor +
      '\'' +
      ", defaultMemory=" +
      defaultMemory +
      ", defaultCpus=" +
      defaultCpus +
      ", defaultDisk=" +
      defaultDisk +
      ", hideNewDeployButton=" +
      hideNewDeployButton +
      ", hideNewRequestButton=" +
      hideNewRequestButton +
      ", loadBalancingEnabled=" +
      loadBalancingEnabled +
      ", title='" +
      title +
      '\'' +
      ", agentHttpPort=" +
      agentHttpPort +
      ", agentHttpsPort=" +
      agentHttpsPort +
      ", defaultBounceExpirationMinutes=" +
      defaultBounceExpirationMinutes +
      ", defaultHealthcheckIntervalSeconds=" +
      defaultHealthcheckIntervalSeconds +
      ", defaultHealthcheckTimeoutSeconds=" +
      defaultHealthcheckTimeoutSeconds +
      ", defaultHealthcheckMaxRetries=" +
      defaultHealthcheckMaxRetries +
      ", defaultStartupTimeoutSeconds=" +
      defaultStartupTimeoutSeconds +
      ", runningTaskLogPath='" +
      runningTaskLogPath +
      '\'' +
      ", finishedTaskLogPath='" +
      finishedTaskLogPath +
      '\'' +
      ", commonHostnameSuffixToOmit='" +
      commonHostnameSuffixToOmit +
      '\'' +
      ", taskS3LogOmitPrefix='" +
      taskS3LogOmitPrefix +
      '\'' +
      ", warnIfScheduledJobIsRunningPastNextRunPct=" +
      warnIfScheduledJobIsRunningPastNextRunPct +
      ", shellCommands='" +
      shellCommands +
      '\'' +
      ", shortenAgentUsageHostname=" +
      shortenAgentUsageHostname +
      ", timestampFormat='" +
      timestampFormat +
      '\'' +
      ", showTaskDiskResource=" +
      showTaskDiskResource +
      ", timestampWithSecondsFormat='" +
      timestampWithSecondsFormat +
      '\'' +
      ", redirectOnUnauthorizedUrl='" +
      redirectOnUnauthorizedUrl +
      '\'' +
      ", extraScript='" +
      extraScript +
      '\'' +
      ", generateAuthHeader=" +
      generateAuthHeader +
      ", authCookieName='" +
      authCookieName +
      '\'' +
      ", authTokenKey='" +
      authTokenKey +
      '\'' +
      ", quickLinks='" +
      quickLinks +
      '\'' +
      ", navTitleLinks='" +
      navTitleLinks +
      '\'' +
      ", lessTerminalPath='" +
      lessTerminalPath +
      '\'' +
      ", appJsPath='" +
      appJsPath +
      '\'' +
      ", appCssPath='" +
      appCssPath +
      '\'' +
      ", vendorJsPath='" +
      vendorJsPath +
      '\'' +
      "} " +
      super.toString()
    );
  }
}
