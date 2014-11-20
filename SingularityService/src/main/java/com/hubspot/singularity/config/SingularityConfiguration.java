package com.hubspot.singularity.config;

import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.SlavePlacement;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SingularityConfiguration extends Configuration {

  @NotNull
  private boolean allowRequestsWithoutOwners = true;

  @NotNull
  private boolean allowTestResourceCalls = false;

  @NotNull
  private long askDriverToKillTasksAgainAfterMillis = TimeUnit.MINUTES.toMillis(5);

  @NotNull
  private long cacheStateForMillis = TimeUnit.SECONDS.toMillis(30);

  @NotNull
  private long checkDeploysEverySeconds = 5;

  @NotNull
  private long checkNewTasksEverySeconds = 5;

  @NotNull
  private int checkNewTasksScheduledThreads = 3;

  @NotNull
  private long checkReconcileWhenRunningEveryMillis = TimeUnit.SECONDS.toMillis(30);

  @NotNull
  private long checkSchedulerEverySeconds = 5;

  @NotNull
  private long checkWebhooksEveryMillis = TimeUnit.SECONDS.toMillis(10);

  @NotNull
  private long cleanupEverySeconds = 5;

  @NotNull
  private long closeWaitSeconds = 5;

  private String commonHostnameSuffixToOmit;

  @NotNull
  private boolean compressLargeDataObjects = true;

  @NotNull
  private long considerTaskHealthyAfterRunningForSeconds = 5;

  @NotNull
  private int cooldownAfterFailures = 3;

  @NotNull
  private double cooldownAfterPctOfInstancesFail = 1.0;

  @NotNull
  private long cooldownExpiresAfterMinutes = 15;

  @NotNull
  private long cooldownMinScheduleSeconds = 120;

  @JsonProperty("database")
  private DataSourceFactory databaseConfiguration;

  @NotNull
  private SlavePlacement defaultSlavePlacement = SlavePlacement.GREEDY;

  @NotNull
  private boolean defaultValueForKillTasksOfPausedRequests = true;

  @NotNull
  private long deltaAfterWhichTasksAreLateMillis = TimeUnit.SECONDS.toMillis(30);

  @NotNull
  private long deployHealthyBySeconds = 120;

  private boolean enableCorsFilter = false;

  @NotNull
  private long healthcheckIntervalSeconds = 5;

  @NotNull
  private int healthcheckStartThreads = 3;

  @NotNull
  private long healthcheckTimeoutSeconds = 5;

  private String hostname;

  @NotNull
  private long killAfterTasksDoNotRunDefaultSeconds = 600;

  @NotNull
  private long killNonLongRunningTasksInCleanupAfterSeconds = TimeUnit.HOURS.toSeconds(24);

  @JsonProperty("loadBalancerQueryParams")
  private Map<String, String> loadBalancerQueryParams;

  @NotNull
  private long loadBalancerRequestTimeoutMillis = 2000;

  private String loadBalancerUri;

  @NotNull
  private int logFetchMaxThreads = 15;

  @NotNull
  private int maxDeployIdSize = 50;

  @NotNull
  private int maxHealthcheckResponseBodyBytes = 8192;

  @NotNull
  private int maxQueuedUpdatesPerWebhook = 50;

  @NotNull
  private int maxRequestIdSize = 100;

  @JsonProperty("mesos")
  private MesosConfiguration mesosConfiguration;

  @NotNull
  private int newTaskCheckerBaseDelaySeconds = 1;

  @NotNull
  private long persistHistoryEverySeconds = TimeUnit.HOURS.toSeconds(1);

  @JsonProperty("s3")
  private S3Configuration s3Configuration;

  @NotNull
  private boolean sandboxDefaultsToTaskId = false;

  @NotNull
  private long sandboxHttpTimeoutMillis = TimeUnit.SECONDS.toMillis(5);

  @NotNull
  private long saveStateEverySeconds = 60;

  @JsonProperty("sentry")
  private SentryConfiguration sentryConfiguration;

  @JsonProperty("smtp")
  private SMTPConfiguration smtpConfiguration;

  @NotNull
  private long startNewReconcileEverySeconds = TimeUnit.MINUTES.toSeconds(10);

  @JsonProperty("ui")
  @Valid
  private UIConfiguration uiConfiguration = new UIConfiguration();

  @NotNull
  private long zookeeperAsyncTimeout = 5000;

  @JsonProperty("zookeeper")
  private ZooKeeperConfiguration zooKeeperConfiguration;

  public boolean allowTestResourceCalls() {
    return allowTestResourceCalls;
  }

  public long getAskDriverToKillTasksAgainAfterMillis() {
    return askDriverToKillTasksAgainAfterMillis;
  }

  public long getCacheStateForMillis() {
    return cacheStateForMillis;
  }

  public long getCheckDeploysEverySeconds() {
    return checkDeploysEverySeconds;
  }

  public long getCheckNewTasksEverySeconds() {
    return checkNewTasksEverySeconds;
  }

  public int getCheckNewTasksScheduledThreads() {
    return checkNewTasksScheduledThreads;
  }

  public long getCheckReconcileWhenRunningEveryMillis() {
    return checkReconcileWhenRunningEveryMillis;
  }

  public long getCheckSchedulerEverySeconds() {
    return checkSchedulerEverySeconds;
  }

  public long getCheckWebhooksEveryMillis() {
    return checkWebhooksEveryMillis;
  }

  public long getCleanupEverySeconds() {
    return cleanupEverySeconds;
  }

  public long getCloseWaitSeconds() {
    return closeWaitSeconds;
  }

  public Optional<String> getCommonHostnameSuffixToOmit() {
    return Optional.fromNullable(commonHostnameSuffixToOmit);
  }

  public long getConsiderTaskHealthyAfterRunningForSeconds() {
    return considerTaskHealthyAfterRunningForSeconds;
  }

  public int getCooldownAfterFailures() {
    return cooldownAfterFailures;
  }

  public double getCooldownAfterPctOfInstancesFail() {
    return cooldownAfterPctOfInstancesFail;
  }

  public long getCooldownExpiresAfterMinutes() {
    return cooldownExpiresAfterMinutes;
  }

  public long getCooldownMinScheduleSeconds() {
    return cooldownMinScheduleSeconds;
  }

  public Optional<DataSourceFactory> getDatabaseConfiguration() {
    return Optional.fromNullable(databaseConfiguration);
  }

  public SlavePlacement getDefaultSlavePlacement() {
    return defaultSlavePlacement;
  }

  public long getDeltaAfterWhichTasksAreLateMillis() {
    return deltaAfterWhichTasksAreLateMillis;
  }

  public long getDeployHealthyBySeconds() {
    return deployHealthyBySeconds;
  }

  public long getHealthcheckIntervalSeconds() {
    return healthcheckIntervalSeconds;
  }

  public int getHealthcheckStartThreads() {
    return healthcheckStartThreads;
  }

  public long getHealthcheckTimeoutSeconds() {
    return healthcheckTimeoutSeconds;
  }

  public String getHostname() {
    return hostname;
  }

  public long getKillAfterTasksDoNotRunDefaultSeconds() {
    return killAfterTasksDoNotRunDefaultSeconds;
  }

  public long getKillNonLongRunningTasksInCleanupAfterSeconds() {
    return killNonLongRunningTasksInCleanupAfterSeconds;
  }

  public Optional<Map<String, String>> getLoadBalancerQueryParams() {
    return Optional.fromNullable(loadBalancerQueryParams);
  }

  public long getLoadBalancerRequestTimeoutMillis() {
    return loadBalancerRequestTimeoutMillis;
  }

  public String getLoadBalancerUri() {
    return loadBalancerUri;
  }

  public int getLogFetchMaxThreads() {
    return logFetchMaxThreads;
  }

  public int getMaxDeployIdSize() {
    return maxDeployIdSize;
  }

  public int getMaxHealthcheckResponseBodyBytes() {
    return maxHealthcheckResponseBodyBytes;
  }

  public int getMaxQueuedUpdatesPerWebhook() {
    return maxQueuedUpdatesPerWebhook;
  }

  public int getMaxRequestIdSize() {
    return maxRequestIdSize;
  }

  public MesosConfiguration getMesosConfiguration() {
    return mesosConfiguration;
  }

  public int getNewTaskCheckerBaseDelaySeconds() {
    return newTaskCheckerBaseDelaySeconds;
  }

  public long getPersistHistoryEverySeconds() {
    return persistHistoryEverySeconds;
  }

  public Optional<S3Configuration> getS3Configuration() {
    return Optional.fromNullable(s3Configuration);
  }

  public long getSandboxHttpTimeoutMillis() {
    return sandboxHttpTimeoutMillis;
  }

  public long getSaveStateEverySeconds() {
    return saveStateEverySeconds;
  }

  public Optional<SentryConfiguration> getSentryConfiguration(){
    return Optional.fromNullable(sentryConfiguration);
  }

  public Optional<SMTPConfiguration> getSmtpConfiguration() {
    return Optional.fromNullable(smtpConfiguration);
  }

  public long getStartNewReconcileEverySeconds() {
    return startNewReconcileEverySeconds;
  }

  public UIConfiguration getUiConfiguration() {
    return uiConfiguration;
  }

  public long getZookeeperAsyncTimeout() {
    return zookeeperAsyncTimeout;
  }

  public ZooKeeperConfiguration getZooKeeperConfiguration() {
    return zooKeeperConfiguration;
  }

  public boolean isAllowRequestsWithoutOwners() {
    return allowRequestsWithoutOwners;
  }

  public boolean isCompressLargeDataObjects() {
    return compressLargeDataObjects;
  }

  public boolean isDefaultValueForKillTasksOfPausedRequests() {
    return defaultValueForKillTasksOfPausedRequests;
  }

  public boolean isEnableCorsFilter() {
    return enableCorsFilter;
  }

  public boolean isSandboxDefaultsToTaskId() {
    return sandboxDefaultsToTaskId;
  }

  public void setAllowRequestsWithoutOwners(boolean allowRequestsWithoutOwners) {
    this.allowRequestsWithoutOwners = allowRequestsWithoutOwners;
  }

  public void setAllowTestResourceCalls(boolean allowTestResourceCalls) {
    this.allowTestResourceCalls = allowTestResourceCalls;
  }

  public void setAskDriverToKillTasksAgainAfterMillis(long askDriverToKillTasksAgainAfterMillis) {
    this.askDriverToKillTasksAgainAfterMillis = askDriverToKillTasksAgainAfterMillis;
  }

  public void setCacheStateForMillis(long cacheStateForMillis) {
    this.cacheStateForMillis = cacheStateForMillis;
  }

  public void setCheckDeploysEverySeconds(long checkDeploysEverySeconds) {
    this.checkDeploysEverySeconds = checkDeploysEverySeconds;
  }

  public void setCheckNewTasksEverySeconds(long checkNewTasksEverySeconds) {
    this.checkNewTasksEverySeconds = checkNewTasksEverySeconds;
  }

  public void setCheckNewTasksScheduledThreads(int checkNewTasksScheduledThreads) {
    this.checkNewTasksScheduledThreads = checkNewTasksScheduledThreads;
  }

  public void setCheckReconcileWhenRunningEveryMillis(long checkReconcileWhenRunningEveryMillis) {
    this.checkReconcileWhenRunningEveryMillis = checkReconcileWhenRunningEveryMillis;
  }

  public void setCheckSchedulerEverySeconds(long checkSchedulerEverySeconds) {
    this.checkSchedulerEverySeconds = checkSchedulerEverySeconds;
  }

  public void setCheckWebhooksEveryMillis(long checkWebhooksEveryMillis) {
    this.checkWebhooksEveryMillis = checkWebhooksEveryMillis;
  }

  public void setCleanupEverySeconds(long cleanupEverySeconds) {
    this.cleanupEverySeconds = cleanupEverySeconds;
  }

  public void setCloseWaitSeconds(long closeWaitSeconds) {
    this.closeWaitSeconds = closeWaitSeconds;
  }

  public void setCommonHostnameSuffixToOmit(String commonHostnameSuffixToOmit) {
    this.commonHostnameSuffixToOmit = commonHostnameSuffixToOmit;
  }

  public void setCompressLargeDataObjects(boolean compressLargeDataObjects) {
    this.compressLargeDataObjects = compressLargeDataObjects;
  }

  public void setConsiderTaskHealthyAfterRunningForSeconds(long considerTaskHealthyAfterRunningForSeconds) {
    this.considerTaskHealthyAfterRunningForSeconds = considerTaskHealthyAfterRunningForSeconds;
  }

  public void setCooldownAfterFailures(int cooldownAfterFailures) {
    this.cooldownAfterFailures = cooldownAfterFailures;
  }

  public void setCooldownAfterPctOfInstancesFail(double cooldownAfterPctOfInstancesFail) {
    this.cooldownAfterPctOfInstancesFail = cooldownAfterPctOfInstancesFail;
  }

  public void setCooldownExpiresAfterMinutes(long cooldownExpiresAfterMinutes) {
    this.cooldownExpiresAfterMinutes = cooldownExpiresAfterMinutes;
  }

  public void setCooldownMinScheduleSeconds(long cooldownMinScheduleSeconds) {
    this.cooldownMinScheduleSeconds = cooldownMinScheduleSeconds;
  }

  public void setDatabaseConfiguration(DataSourceFactory databaseConfiguration) {
    this.databaseConfiguration = databaseConfiguration;
  }

  public void setDefaultSlavePlacement(SlavePlacement defaultSlavePlacement) {
    this.defaultSlavePlacement = defaultSlavePlacement;
  }

  public void setDefaultValueForKillTasksOfPausedRequests(boolean defaultValueForKillTasksOfPausedRequests) {
    this.defaultValueForKillTasksOfPausedRequests = defaultValueForKillTasksOfPausedRequests;
  }

  public void setDeltaAfterWhichTasksAreLateMillis(long deltaAfterWhichTasksAreLateMillis) {
    this.deltaAfterWhichTasksAreLateMillis = deltaAfterWhichTasksAreLateMillis;
  }

  public void setDeployHealthyBySeconds(long deployHealthyBySeconds) {
    this.deployHealthyBySeconds = deployHealthyBySeconds;
  }

  public void setEnableCorsFilter(boolean enableCorsFilter) {
    this.enableCorsFilter = enableCorsFilter;
  }

  public void setHealthcheckIntervalSeconds(long healthcheckIntervalSeconds) {
    this.healthcheckIntervalSeconds = healthcheckIntervalSeconds;
  }

  public void setHealthcheckStartThreads(int healthcheckStartThreads) {
    this.healthcheckStartThreads = healthcheckStartThreads;
  }

  public void setHealthcheckTimeoutSeconds(long healthcheckTimeoutSeconds) {
    this.healthcheckTimeoutSeconds = healthcheckTimeoutSeconds;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public void setKillAfterTasksDoNotRunDefaultSeconds(long killAfterTasksDoNotRunDefaultSeconds) {
    this.killAfterTasksDoNotRunDefaultSeconds = killAfterTasksDoNotRunDefaultSeconds;
  }

  public void setKillNonLongRunningTasksInCleanupAfterSeconds(long killNonLongRunningTasksInCleanupAfterSeconds) {
    this.killNonLongRunningTasksInCleanupAfterSeconds = killNonLongRunningTasksInCleanupAfterSeconds;
  }

  public void setLoadBalancerQueryParams(Map<String, String> loadBalancerQueryParams) {
    this.loadBalancerQueryParams = loadBalancerQueryParams;
  }

  public void setLoadBalancerRequestTimeoutMillis(long loadBalancerRequestTimeoutMillis) {
    this.loadBalancerRequestTimeoutMillis = loadBalancerRequestTimeoutMillis;
  }

  public void setLoadBalancerUri(String loadBalancerUri) {
    this.loadBalancerUri = loadBalancerUri;
  }

  public void setLogFetchMaxThreads(int logFetchMaxThreads) {
    this.logFetchMaxThreads = logFetchMaxThreads;
  }

  public void setMaxDeployIdSize(int maxDeployIdSize) {
    this.maxDeployIdSize = maxDeployIdSize;
  }

  public void setMaxHealthcheckResponseBodyBytes(int maxHealthcheckResponseBodyBytes) {
    this.maxHealthcheckResponseBodyBytes = maxHealthcheckResponseBodyBytes;
  }

  public void setMaxQueuedUpdatesPerWebhook(int maxQueuedUpdatesPerWebhook) {
    this.maxQueuedUpdatesPerWebhook = maxQueuedUpdatesPerWebhook;
  }

  public void setMaxRequestIdSize(int maxRequestIdSize) {
    this.maxRequestIdSize = maxRequestIdSize;
  }

  public void setMesosConfiguration(MesosConfiguration mesosConfiguration) {
    this.mesosConfiguration = mesosConfiguration;
  }

  public void setNewTaskCheckerBaseDelaySeconds(int newTaskCheckerBaseDelaySeconds) {
    this.newTaskCheckerBaseDelaySeconds = newTaskCheckerBaseDelaySeconds;
  }

  public void setPersistHistoryEverySeconds(long persistHistoryEverySeconds) {
    this.persistHistoryEverySeconds = persistHistoryEverySeconds;
  }

  public void setS3Configuration(S3Configuration s3Configuration) {
    this.s3Configuration = s3Configuration;
  }

  public void setSandboxDefaultsToTaskId(boolean sandboxDefaultsToTaskId) {
    this.sandboxDefaultsToTaskId = sandboxDefaultsToTaskId;
  }

  public void setSandboxHttpTimeoutMillis(long sandboxHttpTimeoutMillis) {
    this.sandboxHttpTimeoutMillis = sandboxHttpTimeoutMillis;
  }

  public void setSaveStateEverySeconds(long saveStateEverySeconds) {
    this.saveStateEverySeconds = saveStateEverySeconds;
  }

  public void setSentryConfiguration(SentryConfiguration sentryConfiguration){
    this.sentryConfiguration = sentryConfiguration;
  }

  public void setSmtpConfiguration(SMTPConfiguration smtpConfiguration) {
    this.smtpConfiguration = smtpConfiguration;
  }

  public void setStartNewReconcileEverySeconds(long startNewReconcileEverySeconds) {
    this.startNewReconcileEverySeconds = startNewReconcileEverySeconds;
  }

  public void setUiConfiguration(UIConfiguration uiConfiguration) {
    this.uiConfiguration = uiConfiguration;
  }

  public void setZookeeperAsyncTimeout(long zookeeperAsyncTimeout) {
    this.zookeeperAsyncTimeout = zookeeperAsyncTimeout;
  }

  public void setZooKeeperConfiguration(ZooKeeperConfiguration zooKeeperConfiguration) {
    this.zooKeeperConfiguration = zooKeeperConfiguration;
  }

}
