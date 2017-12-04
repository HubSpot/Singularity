package com.hubspot.singularity.config;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.hubspot.singularity.config.shell.ShellCommandDescriptor;

public class UIConfiguration {

  public static enum RootUrlMode {
    UI_REDIRECT,
    INDEX_CATCHALL,
    DISABLED;

    public static RootUrlMode parse(String value) {
      checkNotNull(value, "value is null");
      value = value.toUpperCase(Locale.ENGLISH);

      for (RootUrlMode rootUrlMode : RootUrlMode.values()) {
        String name = rootUrlMode.name();
        if (name.equals(value) || name.replace("_", "").equals(value)) {
          return rootUrlMode;
        }
      }

      throw new IllegalArgumentException("Value '" + value + "' unknown");
    }
  }

  @NotEmpty
  @JsonProperty
  private String title = "Singularity";

  @JsonProperty
  private Optional<String> navColor = Optional.absent();

  @JsonProperty
  private String baseUrl;

  @NotEmpty
  private String runningTaskLogPath = "stdout";

  @NotEmpty
  private String finishedTaskLogPath = "stdout";

  @JsonProperty
  @NotNull
  private boolean showTaskDiskResource = false;

  @JsonProperty
  @NotNull
  private List<ShellCommandDescriptor> shellCommands = Collections.emptyList();

  private boolean hideNewDeployButton = false;
  private boolean hideNewRequestButton = false;

  private boolean shortenSlaveUsageHostname = false;

  /**
   * If true, the root of the server (http://.../singularity/) will open the UI. Otherwise,
   * the UI URI (http://.../singularity/ui/) must be used.
   */
  @JsonProperty
  private String rootUrlMode = RootUrlMode.INDEX_CATCHALL.name();

  @JsonProperty
  private Optional<String> taskS3LogOmitPrefix = Optional.absent();

  @NotEmpty
  private String timestampFormat = "lll";

  @NotEmpty
  private String timestampWithSecondsFormat = "lll:ss";

  @JsonProperty
  private Optional<String> redirectOnUnauthorizedUrl = Optional.absent();


  @JsonProperty
  private Optional<String> extraScript = Optional.absent();

  @JsonProperty
  private String authTokenKey = "token";

  @JsonProperty
  @NotNull
  private String authCookieName = "";

  @JsonProperty
  private Optional<String> apiRootOverride = Optional.absent();

  @JsonProperty
  private Optional<String> appRootOverride = Optional.absent();

  @JsonProperty
  private Optional<String> staticRootOverride = Optional.absent();

  // e.g. {"request":{"SERVICE":[{"title":"my link","template":"http://example.com/{{request.id}}"}]}}
  @JsonProperty
  private Map<String, Map<String, List<UIQuickLinkConfiguration>>> quickLinks = Collections.emptyMap();

  public boolean isHideNewDeployButton() {
    return hideNewDeployButton;
  }

  public void setHideNewDeployButton(boolean hideNewDeployButton) {
    this.hideNewDeployButton = hideNewDeployButton;
  }

  public boolean isHideNewRequestButton() {
    return hideNewRequestButton;
  }

  public void setHideNewRequestButton(boolean hideNewRequestButton) {
    this.hideNewRequestButton = hideNewRequestButton;
  }

  public boolean isShortenSlaveUsageHostname() {
    return shortenSlaveUsageHostname;
  }

  public void setShortenSlaveUsageHostname(boolean shortenSlaveUsageHostname) {
    this.shortenSlaveUsageHostname = shortenSlaveUsageHostname;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Optional<String> getBaseUrl() {
    return Optional.fromNullable(Strings.emptyToNull(baseUrl));
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public Optional<String> getNavColor() {
    return navColor;
  }

  public void setNavColor(Optional<String> navColor) {
    this.navColor = navColor;
  }

  @Valid
  public RootUrlMode getRootUrlMode() {
    return RootUrlMode.parse(rootUrlMode);
  }

  /**
   * Supports 'uiRedirect', 'indexCatchall' and 'disabled'.
   *
   * <ul>
   * <li>uiRedirect - UI is served off <tt>/ui</tt> path and index redirects there.</li>
   * <li>indexCatchall - UI is served off <tt>/</tt> using a catchall resource.</li>
   * <li>disabled> - UI is served off <tt>/ui> and the root resource is not served at all.</li>
   * </ul>
   *
   * @param rootUrlMode A valid root url mode.
   */
  public void setRootUrlMode(String rootUrlMode) {
    this.rootUrlMode = rootUrlMode;
  }

  public void setRunningTaskLogPath(String runningTaskLogPath) {
    this.runningTaskLogPath = runningTaskLogPath;
  }

  public void setFinishedTaskLogPath(String finishedTaskLogPath) {
    this.finishedTaskLogPath = finishedTaskLogPath;
  }

  public List<ShellCommandDescriptor> getShellCommands() {
    return shellCommands;
  }

  public boolean isShowTaskDiskResource() { return showTaskDiskResource; }

  public void setShowTaskDiskResource(boolean showTaskDiskResource) { this.showTaskDiskResource = showTaskDiskResource; }

  public void setShellCommands(List<ShellCommandDescriptor> shellCommands) {
    this.shellCommands = shellCommands;
  }

  public String getRunningTaskLogPath() {
    return runningTaskLogPath;
  }

  public String getFinishedTaskLogPath() {
    return finishedTaskLogPath;
  }

  public Optional<String> getTaskS3LogOmitPrefix() {
    return taskS3LogOmitPrefix;
  }

  public void setTaskS3LogOmitPrefix(Optional<String> taskS3LogOmitPrefix) {
    this.taskS3LogOmitPrefix = taskS3LogOmitPrefix;
  }

  public String getTimestampFormat() {
    return timestampFormat;
  }

  public void setTimestampFormat(String timestampFormat) {
    this.timestampFormat = timestampFormat;
  }

  public String getTimestampWithSecondsFormat() {
    return timestampWithSecondsFormat;
  }

  public void setTimestampWithSecondsFormat(String timestampWithSecondsFormat) {
    this.timestampWithSecondsFormat = timestampWithSecondsFormat;
  }

  public Optional<String> getRedirectOnUnauthorizedUrl() {
    return redirectOnUnauthorizedUrl;
  }

  public void setRedirectOnUnauthorizedUrl(Optional<String> redirectOnUnauthorizedUrl) {
    this.redirectOnUnauthorizedUrl = redirectOnUnauthorizedUrl;
  }

  public Optional<String> getExtraScript() {
    return extraScript;
  }

  public void setExtraScript(Optional<String> extraScript) {
    this.extraScript = extraScript;
  }

  public String getAuthTokenKey() {
    return authTokenKey;
  }

  public void setAuthTokenKey(String authTokenKey) {
    this.authTokenKey = authTokenKey;
  }

  public String getAuthCookieName() {
    return authCookieName;
  }

  public void setAuthCookieName(String authCookieName) {
    this.authCookieName = authCookieName;
  }

  public Optional<String> getApiRootOverride() {
    return apiRootOverride;
  }

  public void setApiRootOverride(Optional<String> apiRootOverride) {
    this.apiRootOverride = apiRootOverride;
  }

  public Optional<String> getAppRootOverride() {
    return appRootOverride;
  }

  public void setAppRootOverride(Optional<String> appRootOverride) {
    this.appRootOverride = appRootOverride;
  }

  public Optional<String> getStaticRootOverride() {
    return staticRootOverride;
  }

  public void setStaticRootOverride(Optional<String> staticRootOverride) {
    this.staticRootOverride = staticRootOverride;
  }

  public Map<String, Map<String, List<UIQuickLinkConfiguration>>> getQuickLinks() {
    return quickLinks;
  }

  public void setQuickLinks(Map<String, Map<String, List<UIQuickLinkConfiguration>>> quickLinks) {
    this.quickLinks = quickLinks;
  }
}
