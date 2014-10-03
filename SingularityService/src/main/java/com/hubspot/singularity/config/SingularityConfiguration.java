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

@JsonIgnoreProperties(ignoreUnknown = true)
public class SingularityConfiguration extends Configuration {
  public static final UIConfiguration DEFAULT_UI_CONFIGURATION = new UIConfiguration();

  @JsonProperty("mesos")
  private MesosConfiguration mesosConfiguration;

  @JsonProperty("zookeeper")
  private ZooKeeperConfiguration zooKeeperConfiguration;

  @JsonProperty("smtp")
  private SMTPConfiguration smtpConfiguration;

  @JsonProperty("s3")
  private S3Configuration s3Configuration;

  @JsonProperty("hostname")
  private String hostname;

  @JsonProperty("loadBalancerUri")
  private String loadBalancerUri;

  @JsonProperty("loadBalancerQueryParams")
  private Map<String, String> loadBalancerQueryParams;

  @JsonProperty("sentry")
  private SentryConfiguration sentryConfiguration;

  @JsonProperty("ui")
  public UIConfiguration uiConfiguration = DEFAULT_UI_CONFIGURATION;

  public UIConfiguration getUiConfiguration() {
    return uiConfiguration;
  }

  public void setUiConfiguration(UIConfiguration uiConfiguration) {
    this.uiConfiguration = uiConfiguration;
  }

  @Valid
  @NotNull
  private DataSourceFactory database;

  @NotNull
  private long closeWaitSeconds = 5;

  @NotNull
  private long cleanupEverySeconds = 5;

  @NotNull
  private long checkDeploysEverySeconds = 5;

  @NotNull
  private long saveStateEverySeconds = 60;

  @NotNull
  private long healthcheckTimeoutSeconds = 5;

  @NotNull
  private long checkNewTasksEverySeconds = 5;

  @NotNull
  private int checkNewTasksScheduledThreads = 3;

  @NotNull
  private long healthcheckIntervalSeconds = 5;

  @NotNull
  private int newTaskCheckerBaseDelaySeconds = 1;

  @NotNull
  private long killNonLongRunningTasksInCleanupAfterSeconds = TimeUnit.HOURS.toSeconds(24);

  @NotNull
  private long persistHistoryEverySeconds = TimeUnit.HOURS.toSeconds(1);

  @NotNull
  private long deltaAfterWhichTasksAreLateMillis = TimeUnit.SECONDS.toMillis(30);

  @NotNull
  private long killAfterTasksDoNotRunDefaultSeconds = 600;

  @NotNull
  private int logFetchCoreThreads = 3;

  @NotNull
  private int healthcheckStartThreads = 3;

  @NotNull
  private long checkWebhooksEveryMillis = TimeUnit.SECONDS.toMillis(10);

  @NotNull
  private int maxQueuedUpdatesPerWebhook = 50;

  @NotNull
  private int logFetchMaxThreads = 25;

  @NotNull
  private long zookeeperAsyncTimeout = 5000;

  @NotNull
  private long deployHealthyBySeconds = 120;

  @NotNull
  private int maxRequestIdSize = 100;

  @NotNull
  private int maxDeployIdSize = 50;

  @NotNull
  private long loadBalancerRequestTimeoutMillis = 2000;

  @NotNull
  private boolean allowTestResourceCalls = false;

  @NotNull
  private boolean compressLargeDataObjects = true;

  @NotNull
  private long considerTaskHealthyAfterRunningForSeconds = 5;

  @NotNull
  private int maxHealthcheckResponseBodyBytes = 8192;

  @NotNull
  private boolean sandboxDefaultsToTaskId = true;

  @NotNull
  private int cooldownAfterFailures = 5;

  @NotNull
  private long cooldownExpiresAfterMinutes = 30;

  @NotNull
  private long cooldownMinScheduleSeconds = 120;

  @NotNull
  private boolean allowRequestsWithoutOwners = true;

  @NotNull
  private long cacheStateForMillis = TimeUnit.SECONDS.toMillis(30);

  @NotNull
  private boolean defaultValueForKillTasksOfPausedRequests = true;

  @NotNull
  private long askDriverToKillTasksAgainAfterMillis = TimeUnit.MINUTES.toMillis(5);

  private boolean enableCorsFilter = false;

  public long getAskDriverToKillTasksAgainAfterMillis() {
    return askDriverToKillTasksAgainAfterMillis;
  }

  public void setAskDriverToKillTasksAgainAfterMillis(long askDriverToKillTasksAgainAfterMillis) {
    this.askDriverToKillTasksAgainAfterMillis = askDriverToKillTasksAgainAfterMillis;
  }

  public long getCheckWebhooksEveryMillis() {
    return checkWebhooksEveryMillis;
  }

  public void setCheckWebhooksEveryMillis(long checkWebhooksEveryMillis) {
    this.checkWebhooksEveryMillis = checkWebhooksEveryMillis;
  }

  public int getMaxQueuedUpdatesPerWebhook() {
    return maxQueuedUpdatesPerWebhook;
  }

  public void setMaxQueuedUpdatesPerWebhook(int maxQueuedUpdatesPerWebhook) {
    this.maxQueuedUpdatesPerWebhook = maxQueuedUpdatesPerWebhook;
  }

  public long getCacheStateForMillis() {
    return cacheStateForMillis;
  }

  public boolean isDefaultValueForKillTasksOfPausedRequests() {
    return defaultValueForKillTasksOfPausedRequests;
  }

  public void setDefaultValueForKillTasksOfPausedRequests(boolean defaultValueForKillTasksOfPausedRequests) {
    this.defaultValueForKillTasksOfPausedRequests = defaultValueForKillTasksOfPausedRequests;
  }

  public void setCacheStateForMillis(long cacheStateForMillis) {
    this.cacheStateForMillis = cacheStateForMillis;
  }

  public boolean allowTestResourceCalls() {
    return allowTestResourceCalls;
  }

  public void setAllowTestResourceCalls(boolean allowTestResourceCalls) {
    this.allowTestResourceCalls = allowTestResourceCalls;
  }

  public boolean isAllowRequestsWithoutOwners() {
    return allowRequestsWithoutOwners;
  }

  public void setAllowRequestsWithoutOwners(boolean allowRequestsWithoutOwners) {
    this.allowRequestsWithoutOwners = allowRequestsWithoutOwners;
  }

  public int getCooldownAfterFailures() {
    return cooldownAfterFailures;
  }

  public void setCooldownAfterFailures(int cooldownAfterFailures) {
    this.cooldownAfterFailures = cooldownAfterFailures;
  }

  public long getCooldownExpiresAfterMinutes() {
    return cooldownExpiresAfterMinutes;
  }

  public void setCooldownExpiresAfterMinutes(long cooldownExpiresAfterMinutes) {
    this.cooldownExpiresAfterMinutes = cooldownExpiresAfterMinutes;
  }

  public long getCooldownMinScheduleSeconds() {
    return cooldownMinScheduleSeconds;
  }

  public void setCooldownMinScheduleSeconds(long cooldownMinScheduleSeconds) {
    this.cooldownMinScheduleSeconds = cooldownMinScheduleSeconds;
  }

  public boolean isSandboxDefaultsToTaskId() {
    return sandboxDefaultsToTaskId;
  }

  public void setSandboxDefaultsToTaskId(boolean sandboxDefaultsToTaskId) {
    this.sandboxDefaultsToTaskId = sandboxDefaultsToTaskId;
  }

  public int getMaxHealthcheckResponseBodyBytes() {
    return maxHealthcheckResponseBodyBytes;
  }

  public void setMaxHealthcheckResponseBodyBytes(int maxHealthcheckResponseBodyBytes) {
    this.maxHealthcheckResponseBodyBytes = maxHealthcheckResponseBodyBytes;
  }

  public long getConsiderTaskHealthyAfterRunningForSeconds() {
    return considerTaskHealthyAfterRunningForSeconds;
  }

  public void setConsiderTaskHealthyAfterRunningForSeconds(long considerTaskHealthyAfterRunningForSeconds) {
    this.considerTaskHealthyAfterRunningForSeconds = considerTaskHealthyAfterRunningForSeconds;
  }

  public boolean isCompressLargeDataObjects() {
    return compressLargeDataObjects;
  }

  public void setCompressLargeDataObjects(boolean compressLargeDataObjects) {
    this.compressLargeDataObjects = compressLargeDataObjects;
  }

  public long getCheckNewTasksEverySeconds() {
    return checkNewTasksEverySeconds;
  }

  public void setCheckNewTasksEverySeconds(long checkNewTasksEverySeconds) {
    this.checkNewTasksEverySeconds = checkNewTasksEverySeconds;
  }

  public int getCheckNewTasksScheduledThreads() {
    return checkNewTasksScheduledThreads;
  }

  public void setCheckNewTasksScheduledThreads(int checkNewTasksScheduledThreads) {
    this.checkNewTasksScheduledThreads = checkNewTasksScheduledThreads;
  }

  public long getLoadBalancerRequestTimeoutMillis() {
    return loadBalancerRequestTimeoutMillis;
  }

  public void setLoadBalancerRequestTimeoutMillis(long loadBalancerRequestTimeoutMillis) {
    this.loadBalancerRequestTimeoutMillis = loadBalancerRequestTimeoutMillis;
  }

  public String getLoadBalancerUri() {
    return loadBalancerUri;
  }

  public void setLoadBalancerUri(String loadBalancerUri) {
    this.loadBalancerUri = loadBalancerUri;
  }

  public int getMaxRequestIdSize() {
    return maxRequestIdSize;
  }

  public void setMaxRequestIdSize(int maxRequestIdSize) {
    this.maxRequestIdSize = maxRequestIdSize;
  }

  public int getMaxDeployIdSize() {
    return maxDeployIdSize;
  }

  public void setMaxDeployIdSize(int maxDeployIdSize) {
    this.maxDeployIdSize = maxDeployIdSize;
  }

  public long getHealthcheckIntervalSeconds() {
    return healthcheckIntervalSeconds;
  }

  public void setHealthcheckIntervalSeconds(long healthcheckIntervalSeconds) {
    this.healthcheckIntervalSeconds = healthcheckIntervalSeconds;
  }

  public long getKillNonLongRunningTasksInCleanupAfterSeconds() {
    return killNonLongRunningTasksInCleanupAfterSeconds;
  }

  public void setKillNonLongRunningTasksInCleanupAfterSeconds(long killNonLongRunningTasksInCleanupAfterSeconds) {
    this.killNonLongRunningTasksInCleanupAfterSeconds = killNonLongRunningTasksInCleanupAfterSeconds;
  }

  public long getDeployHealthyBySeconds() {
    return deployHealthyBySeconds;
  }

  public int getHealthcheckStartThreads() {
    return healthcheckStartThreads;
  }

  public void setHealthcheckStartThreads(int healthcheckStartThreads) {
    this.healthcheckStartThreads = healthcheckStartThreads;
  }

  public void setDeployHealthyBySeconds(long deployHealthyBySeconds) {
    this.deployHealthyBySeconds = deployHealthyBySeconds;
  }

  public long getPersistHistoryEverySeconds() {
    return persistHistoryEverySeconds;
  }

  public void setPersistHistoryEverySeconds(long persistHistoryEverySeconds) {
    this.persistHistoryEverySeconds = persistHistoryEverySeconds;
  }

  public long getCheckDeploysEverySeconds() {
    return checkDeploysEverySeconds;
  }

  public long getHealthcheckTimeoutSeconds() {
    return healthcheckTimeoutSeconds;
  }

  public void setHealthcheckTimeoutSeconds(long healthcheckTimeoutSeconds) {
    this.healthcheckTimeoutSeconds = healthcheckTimeoutSeconds;
  }

  public long getCleanupEverySeconds() {
    return cleanupEverySeconds;
  }

  public long getCloseWaitSeconds() {
    return closeWaitSeconds;
  }

  @JsonProperty("database")
  public DataSourceFactory getDataSourceFactory() {
    return database;
  }

  public long getDeltaAfterWhichTasksAreLateMillis() {
    return deltaAfterWhichTasksAreLateMillis;
  }

  public String getHostname() {
    return hostname;
  }

  public long getKillAfterTasksDoNotRunDefaultSeconds() {
    return killAfterTasksDoNotRunDefaultSeconds;
  }

  public int getLogFetchCoreThreads() {
    return logFetchCoreThreads;
  }

  public int getLogFetchMaxThreads() {
    return logFetchMaxThreads;
  }

  public MesosConfiguration getMesosConfiguration() {
    return mesosConfiguration;
  }

  public long getSaveStateEverySeconds() {
    return saveStateEverySeconds;
  }

  public Optional<S3Configuration> getS3Configuration() {
    return Optional.fromNullable(s3Configuration);
  }

  public Optional<SMTPConfiguration> getSmtpConfiguration() {
    return Optional.fromNullable(smtpConfiguration);
  }

  public long getZookeeperAsyncTimeout() {
    return zookeeperAsyncTimeout;
  }

  public ZooKeeperConfiguration getZooKeeperConfiguration() {
    return zooKeeperConfiguration;
  }

  public void setCheckDeploysEverySeconds(long checkDeploysEverySeconds) {
    this.checkDeploysEverySeconds = checkDeploysEverySeconds;
  }

  public int getNewTaskCheckerBaseDelaySeconds() {
    return newTaskCheckerBaseDelaySeconds;
  }

  public void setNewTaskCheckerBaseDelaySeconds(int newTaskCheckerBaseDelaySeconds) {
    this.newTaskCheckerBaseDelaySeconds = newTaskCheckerBaseDelaySeconds;
  }

  public void setCleanupEverySeconds(long cleanupEverySeconds) {
    this.cleanupEverySeconds = cleanupEverySeconds;
  }

  public void setCloseWaitSeconds(long closeWaitSeconds) {
    this.closeWaitSeconds = closeWaitSeconds;
  }

  @JsonProperty("database")
  public void setDataSourceFactory(DataSourceFactory dataSourceFactory) {
    this.database = dataSourceFactory;
  }

  public void setDeltaAfterWhichTasksAreLateMillis(long deltaAfterWhichTasksAreLateMillis) {
    this.deltaAfterWhichTasksAreLateMillis = deltaAfterWhichTasksAreLateMillis;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public void setKillAfterTasksDoNotRunDefaultSeconds(long killAfterTasksDoNotRunDefaultSeconds) {
    this.killAfterTasksDoNotRunDefaultSeconds = killAfterTasksDoNotRunDefaultSeconds;
  }

  public void setLogFetchCoreThreads(int logFetchCoreThreads) {
    this.logFetchCoreThreads = logFetchCoreThreads;
  }

  public void setLogFetchMaxThreads(int logFetchMaxThreads) {
    this.logFetchMaxThreads = logFetchMaxThreads;
  }

  public void setMesosConfiguration(MesosConfiguration mesosConfiguration) {
    this.mesosConfiguration = mesosConfiguration;
  }

  public void setSaveStateEverySeconds(long saveStateEverySeconds) {
    this.saveStateEverySeconds = saveStateEverySeconds;
  }

  public Optional<SentryConfiguration> getSentryConfiguration(){
    return Optional.fromNullable(sentryConfiguration);
  }

  public void setSentryConfiguration(SentryConfiguration sentryConfiguration){
    this.sentryConfiguration = sentryConfiguration;
  }

  public void setS3Configuration(S3Configuration s3Configuration) {
    this.s3Configuration = s3Configuration;
  }

  public void setSmtpConfiguration(SMTPConfiguration smtpConfiguration) {
    this.smtpConfiguration = smtpConfiguration;
  }

  public void setZookeeperAsyncTimeout(long zookeeperAsyncTimeout) {
    this.zookeeperAsyncTimeout = zookeeperAsyncTimeout;
  }

  public void setZooKeeperConfiguration(ZooKeeperConfiguration zooKeeperConfiguration) {
    this.zooKeeperConfiguration = zooKeeperConfiguration;
  }

  public Optional<Map<String, String>> getLoadBalancerQueryParams() {
    return Optional.fromNullable(loadBalancerQueryParams);
  }

  public void setLoadBalancerQueryParams(Map<String, String> loadBalancerQueryParams) {
    this.loadBalancerQueryParams = loadBalancerQueryParams;
  }

  public boolean isEnableCorsFilter() {
    return enableCorsFilter;
  }

  public void setEnableCorsFilter(boolean enableCorsFilter) {
    this.enableCorsFilter = enableCorsFilter;
  }
}
