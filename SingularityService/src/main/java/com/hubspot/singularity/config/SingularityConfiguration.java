package com.hubspot.singularity.config;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.hubspot.singularity.SlavePlacement;

import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SingularityConfiguration extends Configuration {

  private boolean allowRequestsWithoutOwners = true;

  private boolean allowTestResourceCalls = false;

  private long askDriverToKillTasksAgainAfterMillis = TimeUnit.MINUTES.toMillis(5);

  private long cacheStateForMillis = TimeUnit.SECONDS.toMillis(30);

  private long checkDeploysEverySeconds = 5;

  private long checkNewTasksEverySeconds = 5;

  private int checkNewTasksScheduledThreads = 3;

  private long checkReconcileWhenRunningEveryMillis = TimeUnit.SECONDS.toMillis(30);

  private long checkScheduledJobsEveryMillis = TimeUnit.MINUTES.toMillis(10);

  private long checkSchedulerEverySeconds = 5;

  private long checkWebhooksEveryMillis = TimeUnit.SECONDS.toMillis(10);

  private long cleanupEverySeconds = 5;

  private long closeWaitSeconds = 5;

  private String commonHostnameSuffixToOmit;

  private boolean compressLargeDataObjects = true;

  private long considerTaskHealthyAfterRunningForSeconds = 5;

  private int cooldownAfterFailures = 3;

  private double cooldownAfterPctOfInstancesFail = 1.0;

  private long cooldownExpiresAfterMinutes = 15;

  private long cooldownMinScheduleSeconds = 120;

  @JsonProperty("database")
  private DataSourceFactory databaseConfiguration;

  @NotNull
  private SlavePlacement defaultSlavePlacement = SlavePlacement.GREEDY;

  private boolean defaultValueForKillTasksOfPausedRequests = true;

  private long deleteDeploysFromZkWhenNoDatabaseAfterHours = TimeUnit.DAYS.toHours(14);

  private long deleteStaleRequestsFromZkWhenNoDatabaseAfterHours = TimeUnit.DAYS.toHours(14);

  private long deleteTasksFromZkWhenNoDatabaseAfterHours = TimeUnit.DAYS.toHours(7);

  private long deleteUndeliverableWebhooksAfterHours = TimeUnit.DAYS.toHours(7);

  private long deltaAfterWhichTasksAreLateMillis = TimeUnit.SECONDS.toMillis(30);

  private long deployHealthyBySeconds = 120;

  private boolean enableCorsFilter = false;

  private long healthcheckIntervalSeconds = 5;

  private int healthcheckStartThreads = 3;

  private long healthcheckTimeoutSeconds = 5;

  private String hostname;

  private long killAfterTasksDoNotRunDefaultSeconds = 600;

  private long killNonLongRunningTasksInCleanupAfterSeconds = TimeUnit.HOURS.toSeconds(24);

  private int listenerThreadpoolSize = 3;

  @JsonProperty("loadBalancerQueryParams")
  private Map<String, String> loadBalancerQueryParams;

  private long loadBalancerRequestTimeoutMillis = 2000;

  private String loadBalancerUri;

  private int logFetchMaxThreads = 15;

  private int maxDeployIdSize = 50;

  private int maxHealthcheckResponseBodyBytes = 8192;

  private int maxQueuedUpdatesPerWebhook = 50;

  private int maxRequestIdSize = 100;

  @JsonProperty("mesos")
  @Valid
  private MesosConfiguration mesosConfiguration;

  private int newTaskCheckerBaseDelaySeconds = 1;

  private long persistHistoryEverySeconds = TimeUnit.HOURS.toSeconds(1);

  @JsonProperty("s3")
  private S3Configuration s3Configuration;

  private boolean sandboxDefaultsToTaskId = false;

  private long sandboxHttpTimeoutMillis = TimeUnit.SECONDS.toMillis(5);

  private long saveStateEverySeconds = 60;

  @JsonProperty("sentry")
  private SentryConfiguration sentryConfiguration;

  @JsonProperty("smtp")
  private SMTPConfiguration smtpConfiguration;

  private long startNewReconcileEverySeconds = TimeUnit.MINUTES.toSeconds(10);

  @JsonProperty("ui")
  @Valid
  private UIConfiguration uiConfiguration = new UIConfiguration();

  /** If true, the event system waits for all listeners having processed an event. */
  private boolean waitForListeners = true;

  private long warnIfScheduledJobIsRunningForAtLeastMillis = TimeUnit.DAYS.toMillis(1);

  private int warnIfScheduledJobIsRunningPastNextRunPct = 200;

  private long zookeeperAsyncTimeout = 5000;

  private int coreThreadpoolSize = 8;

  private long threadpoolShutdownDelayInSeconds = 1;

  @Valid
  @JsonProperty("customExecutor")
  @NotNull
  private CustomExecutorConfiguration customExecutorConfiguration = new CustomExecutorConfiguration();

  private boolean createDeployIds = false;

  @Min(4)
  @Max(32)
  private int deployIdLength = 8;

  @JsonProperty("zookeeper")
  @Valid
  private ZooKeeperConfiguration zooKeeperConfiguration;

  @JsonProperty("ldap")
  @NotNull
  private LDAPConfiguration ldapConfiguration = LDAPConfiguration.disabled();

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

  public long getCheckScheduledJobsEveryMillis() {
    return checkScheduledJobsEveryMillis;
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
    return Optional.fromNullable(Strings.emptyToNull(commonHostnameSuffixToOmit));
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

  public int getCoreThreadpoolSize() {
    return coreThreadpoolSize;
  }

  public Optional<DataSourceFactory> getDatabaseConfiguration() {
    return Optional.fromNullable(databaseConfiguration);
  }

  public SlavePlacement getDefaultSlavePlacement() {
    return defaultSlavePlacement;
  }

  public long getDeleteDeploysFromZkWhenNoDatabaseAfterHours() {
    return deleteDeploysFromZkWhenNoDatabaseAfterHours;
  }

  public long getDeleteStaleRequestsFromZkWhenNoDatabaseAfterHours() {
    return deleteStaleRequestsFromZkWhenNoDatabaseAfterHours;
  }

  public long getDeleteTasksFromZkWhenNoDatabaseAfterHours() {
    return deleteTasksFromZkWhenNoDatabaseAfterHours;
  }

  public long getDeleteUndeliverableWebhooksAfterHours() {
    return deleteUndeliverableWebhooksAfterHours;
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

  public Optional<String> getHostname() {
    return Optional.fromNullable(Strings.emptyToNull(hostname));
  }

  public long getKillAfterTasksDoNotRunDefaultSeconds() {
    return killAfterTasksDoNotRunDefaultSeconds;
  }

  public long getKillNonLongRunningTasksInCleanupAfterSeconds() {
    return killNonLongRunningTasksInCleanupAfterSeconds;
  }

  public int getListenerThreadpoolSize() {
    return listenerThreadpoolSize;
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

  public long getThreadpoolShutdownDelayInSeconds() {
    return threadpoolShutdownDelayInSeconds;
  }

  public UIConfiguration getUiConfiguration() {
    return uiConfiguration;
  }

  public long getWarnIfScheduledJobIsRunningForAtLeastMillis() {
    return warnIfScheduledJobIsRunningForAtLeastMillis;
  }

  public int getWarnIfScheduledJobIsRunningPastNextRunPct() {
    return warnIfScheduledJobIsRunningPastNextRunPct;
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

  public boolean isAllowTestResourceCalls() {
    return allowTestResourceCalls;
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

  public boolean isWaitForListeners() {
    return waitForListeners;
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

  public void setCheckScheduledJobsEveryMillis(long checkScheduledJobsEveryMillis) {
    this.checkScheduledJobsEveryMillis = checkScheduledJobsEveryMillis;
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

  public void setCoreThreadpoolSize(int coreThreadpoolSize) {
    this.coreThreadpoolSize = coreThreadpoolSize;
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

  public void setDeleteDeploysFromZkWhenNoDatabaseAfterHours(long deleteDeploysFromZkWhenNoDatabaseAfterHours) {
    this.deleteDeploysFromZkWhenNoDatabaseAfterHours = deleteDeploysFromZkWhenNoDatabaseAfterHours;
  }

  public void setDeleteStaleRequestsFromZkWhenNoDatabaseAfterHours(long deleteStaleRequestsFromZkWhenNoDatabaseAfterHours) {
    this.deleteStaleRequestsFromZkWhenNoDatabaseAfterHours = deleteStaleRequestsFromZkWhenNoDatabaseAfterHours;
  }

  public void setDeleteTasksFromZkWhenNoDatabaseAfterHours(long deleteTasksFromZkWhenNoDatabaseAfterHours) {
    this.deleteTasksFromZkWhenNoDatabaseAfterHours = deleteTasksFromZkWhenNoDatabaseAfterHours;
  }

  public void setDeleteUndeliverableWebhooksAfterHours(long deleteUndeliverableWebhooksAfterHours) {
    this.deleteUndeliverableWebhooksAfterHours = deleteUndeliverableWebhooksAfterHours;
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

  public void setListenerThreadpoolSize(int listenerThreadpoolSize) {
    this.listenerThreadpoolSize = listenerThreadpoolSize;
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

  public void setWaitForListeners(boolean waitForListeners) {
    this.waitForListeners = waitForListeners;
  }

  public void setWarnIfScheduledJobIsRunningForAtLeastMillis(long warnIfScheduledJobIsRunningForAtLeastMillis) {
    this.warnIfScheduledJobIsRunningForAtLeastMillis = warnIfScheduledJobIsRunningForAtLeastMillis;
  }

  public void setWarnIfScheduledJobIsRunningPastNextRunPct(int warnIfScheduledJobIsRunningPastNextRunPct) {
    this.warnIfScheduledJobIsRunningPastNextRunPct = warnIfScheduledJobIsRunningPastNextRunPct;
  }

  public void setZookeeperAsyncTimeout(long zookeeperAsyncTimeout) {
    this.zookeeperAsyncTimeout = zookeeperAsyncTimeout;
  }

  public void setZooKeeperConfiguration(ZooKeeperConfiguration zooKeeperConfiguration) {
    this.zooKeeperConfiguration = zooKeeperConfiguration;
  }

  public CustomExecutorConfiguration getCustomExecutorConfiguration() {
    return customExecutorConfiguration;
  }

  public void setCustomExecutorConfiguration(CustomExecutorConfiguration customExecutorConfiguration) {
    this.customExecutorConfiguration = customExecutorConfiguration;
  }

  public boolean isCreateDeployIds() {
    return createDeployIds;
  }

  public void setCreateDeployIds(boolean createDeployIds) {
    this.createDeployIds = createDeployIds;
  }

  public int getDeployIdLength() {
    return deployIdLength;
  }

  public void setDeployIdLength(int deployIdLength) {
    this.deployIdLength = deployIdLength;
  }

  public LDAPConfiguration getLdapConfiguration() {
    return ldapConfiguration;
  }

  public void setLdapConfiguration(LDAPConfiguration ldapConfiguration) {
    this.ldapConfiguration = ldapConfiguration;
  }
}
