package com.hubspot.singularity.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SlavePlacement;

import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SingularityConfiguration extends Configuration {

  private boolean allowRequestsWithoutOwners = true;

  private boolean allowTestResourceCalls = false;

  private long askDriverToKillTasksAgainAfterMillis = TimeUnit.MINUTES.toMillis(5);

  private long cacheOffersForMillis = TimeUnit.MINUTES.toMillis(1);

  private int offerCacheSize = 125;

  private boolean cacheOffers = false;

  private long cacheForWebForMillis = TimeUnit.SECONDS.toMillis(30);

  private int cacheTasksMaxSize = 5000;

  private int cacheTasksInitialSize = 100;

  private long cacheTasksForMillis = TimeUnit.DAYS.toMillis(1);

  private int cacheDeploysMaxSize = 2000;

  private int cacheDeploysInitialSize = 100;

  private long cacheDeploysForMillis = TimeUnit.DAYS.toMillis(5);

  private long cacheStateForMillis = TimeUnit.SECONDS.toMillis(30);

  private long checkDeploysEverySeconds = 5;

  private long checkAutoSpreadAllSlavesEverySeconds = 30;

  private long checkNewTasksEverySeconds = 5;

  private long checkExpiringUserActionEveryMillis = TimeUnit.SECONDS.toMillis(45);

  private int checkNewTasksScheduledThreads = 3;

  private long checkReconcileWhenRunningEveryMillis = TimeUnit.SECONDS.toMillis(30);

  private long checkJobsEveryMillis = TimeUnit.MINUTES.toMillis(10);

  private long checkSchedulerEverySeconds = 5;

  private long checkWebhooksEveryMillis = TimeUnit.SECONDS.toMillis(10);

  private long checkUsageEveryMillis = TimeUnit.MINUTES.toMillis(1);

  private int usageIntervalSeconds = 5760; // 15 saved each 5760 seconds (96 min) apart is 1 day of usage

  private long cleanUsageEveryMillis = TimeUnit.MINUTES.toMillis(5);

  private int numUsageToKeep = 15;

  private long cleanupEverySeconds = 5;

  private long checkQueuedMailsEveryMillis = TimeUnit.SECONDS.toMillis(15);

  private boolean ldapCacheEnabled = true;

  private long ldapCacheSize = 100;

  private long ldapCacheExpireMillis = TimeUnit.MINUTES.toMillis(1);

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

  @Min(value = 1, message = "Must be positive and non-zero")
  private int defaultBounceExpirationMinutes = 60;

  @NotNull
  private SlavePlacement defaultSlavePlacement = SlavePlacement.GREEDY;

  @Min(value = 0, message = "Must be non-negative")
  private double placementLeniency = 0.09d;

  private boolean defaultValueForKillTasksOfPausedRequests = true;

  private int defaultDeployStepWaitTimeMs = 0;

  private int defaultDeployMaxTaskRetries = 0;

  private long deleteDeploysFromZkWhenNoDatabaseAfterHours = TimeUnit.DAYS.toHours(14);

  private Optional<Integer> maxStaleDeploysPerRequestInZkWhenNoDatabase = Optional.absent();

  private long deleteDeadSlavesAfterHours = TimeUnit.DAYS.toHours(7);

  private long deleteStaleRequestsFromZkWhenNoDatabaseAfterHours = TimeUnit.DAYS.toHours(14);

  private Optional<Integer> maxRequestsWithHistoryInZkWhenNoDatabase = Optional.absent();

  private long deleteTasksFromZkWhenNoDatabaseAfterHours = TimeUnit.DAYS.toHours(7);

  private Optional<Integer> maxStaleTasksPerRequestInZkWhenNoDatabase = Optional.absent();

  private long deleteUndeliverableWebhooksAfterHours = TimeUnit.DAYS.toHours(7);

  private long deltaAfterWhichTasksAreLateMillis = TimeUnit.SECONDS.toMillis(30);

  private long deployHealthyBySeconds = 120;

  private int dispatchTaskShellCommandsEverySeconds = 5;

  private long debugCuratorCallOverBytes = 25000;

  private long debugCuratorCallOverMillis = 250;

  private boolean enableCorsFilter = false;

  private int healthcheckIntervalSeconds = 5;

  private int healthcheckStartThreads = 3;

  private int healthcheckTimeoutSeconds = 5;

  private Optional<Integer> startupDelaySeconds = Optional.absent();

  private int startupTimeoutSeconds = 45;

  private int startupIntervalSeconds = 2;

  private Optional<Integer> healthcheckMaxRetries = Optional.absent();

  private Optional<Integer> healthcheckMaxTotalTimeoutSeconds = Optional.absent();

  private long killTaskIfNotHealthyAfterSeconds = 600;

  @NotNull
  private List<Integer> healthcheckFailureStatusCodes = Collections.emptyList();

  private String hostname;

  private long killAfterTasksDoNotRunDefaultSeconds = 600;

  private long killNonLongRunningTasksInCleanupAfterSeconds = TimeUnit.HOURS.toSeconds(24);

  private int listenerThreadpoolSize = 3;

  @JsonProperty("loadBalancerQueryParams")
  private Map<String, String> loadBalancerQueryParams;

  private long loadBalancerRequestTimeoutMillis = 2000;

  private long loadBalancerRemovalGracePeriodMillis = 0;

  private String loadBalancerUri;

  private boolean deleteRemovedRequestsFromLoadBalancer = false;

  private Optional<String> taskLabelForLoadBalancerUpstreamGroup = Optional.absent();

  private int logFetchMaxThreads = 15;

  private int maxDeployIdSize = 50;

  private int maxHealthcheckResponseBodyBytes = 8192;

  private int maxQueuedUpdatesPerWebhook = 50;

  private int maxTasksPerOffer = 0;

  private int maxTasksPerOfferPerRequest = 0;

  private double longRunningUsedCpuWeightForOffer = 0.25;

  private double longRunningUsedMemWeightForOffer = 0.65;

  private double longRunningUsedDiskWeightForOffer = 0.10;

  private double freeCpuWeightForOffer = 0.25;

  private double freeMemWeightForOffer = 0.65;

  private double freeDiskWeightForOffer = 0.10;

  private double defaultOfferScoreForMissingUsage = 0.30;

  private long considerNonLongRunningTaskLongRunningAfterRunningForSeconds = TimeUnit.HOURS.toSeconds(6);

  private double maxNonLongRunningUsedResourceWeight = 0.50;

  private int maxRequestIdSize = 100;

  private int maxUserIdSize = 100;

  private boolean storeAllMesosTaskInfoForDebugging = false;

  @JsonProperty("historyPurging")
  @Valid
  private HistoryPurgingConfiguration historyPurgingConfiguration = new HistoryPurgingConfiguration();

  @JsonProperty("mesos")
  @Valid
  private MesosConfiguration mesosConfiguration;

  @JsonProperty("network")
  @Valid
  private NetworkConfiguration networkConfiguration = new NetworkConfiguration();

  private int newTaskCheckerBaseDelaySeconds = 1;

  private long pendingDeployHoldTaskDuringDecommissionMillis = TimeUnit.MINUTES.toMillis(10);

  private long persistHistoryEverySeconds = TimeUnit.HOURS.toSeconds(1);

  private long reconcileSlavesEveryMinutes = TimeUnit.HOURS.toMinutes(1);

  @JsonProperty("s3")
  private S3Configuration s3Configuration;

  private boolean sandboxDefaultsToTaskId = false;

  private long sandboxHttpTimeoutMillis = TimeUnit.SECONDS.toMillis(2);

  private long saveStateEverySeconds = 60;

  @JsonProperty("sentry")
  @Valid
  private SentryConfiguration sentryConfiguration;

  @JsonProperty("taskMetadata")
  @Valid
  private SingularityTaskMetadataConfiguration taskMetadataConfiguration = new SingularityTaskMetadataConfiguration();

  @JsonProperty("smtp")
  @Valid
  private SMTPConfiguration smtpConfiguration;

  private long startNewReconcileEverySeconds = TimeUnit.MINUTES.toSeconds(10);

  @JsonProperty("ui")
  @Valid
  private UIConfiguration uiConfiguration = new UIConfiguration();

  /** If true, the event system waits for all listeners having processed an event. */
  private boolean waitForListeners = true;

  private long warnIfScheduledJobIsRunningForAtLeastMillis = TimeUnit.DAYS.toMillis(1);

  @JsonProperty("taskExecutionTimeLimitMillis")
  private Optional<Long> taskExecutionTimeLimitMillis = Optional.absent();

  private int warnIfScheduledJobIsRunningPastNextRunPct = 200;

  private long zookeeperAsyncTimeout = 5000;

  private int coreThreadpoolSize = 8;

  private long threadpoolShutdownDelayInSeconds = 1;

  private long taskPersistAfterStartupBufferMillis = TimeUnit.MINUTES.toMillis(1);

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
  @Valid
  private LDAPConfiguration ldapConfiguration;

  @JsonProperty("webhookAuth")
  @Valid
  private WebhookAuthConfiguration webhookAuthConfiguration = new WebhookAuthConfiguration();

  @JsonProperty("auth")
  @NotNull
  @Valid
  private AuthConfiguration authConfiguration = new AuthConfiguration();

  @NotNull
  private Map<String, List<String>> reserveSlavesWithAttributes = Collections.emptyMap();

  @JsonProperty("graphite")
  @NotNull
  @Valid
  private GraphiteConfiguration graphiteConfiguration = new GraphiteConfiguration();

  private boolean taskHistoryQueryUsesZkFirst = false;

  @JsonProperty("disasterDetection")
  @NotNull
  @Valid
  private DisasterDetectionConfiguration disasterDetection = new DisasterDetectionConfiguration();

  @Min(0)
  @Max(1)
  private double defaultTaskPriorityLevel = 0.3;

  @NotNull
  private Map<RequestType, Double> defaultTaskPriorityLevelForRequestType = ImmutableMap.of(RequestType.WORKER, 0.5, RequestType.SERVICE, 0.7);

  @Min(0)
  private long checkPriorityKillsEveryMillis = TimeUnit.SECONDS.toMillis(30);

  @Min(0)
  @Max(5)
  private double schedulerPriorityWeightFactor = 1.0;

  private boolean rebalanceRacksOnScaleDown = false;

  private boolean allowBounceToSameHost = false;

  private int maxActiveOnDemandTasksPerRequest = 0;

  private int maxDecommissioningSlaves = 2;

  private boolean spreadAllSlavesEnabled = false;

  private long delayPollersWhenDeltaOverMs = 15000;

  private boolean delayOfferProcessingForLargeStatusUpdateDelta = true;

  private int maxRunNowTaskLaunchDelayDays = 30;

  public long getAskDriverToKillTasksAgainAfterMillis() {
    return askDriverToKillTasksAgainAfterMillis;
  }

  public long getCacheStateForMillis() {
    return cacheStateForMillis;
  }

  public long getDispatchTaskShellCommandsEverySeconds() {
    return dispatchTaskShellCommandsEverySeconds;
  }

  public long getCheckDeploysEverySeconds() {
    return checkDeploysEverySeconds;
  }

  public long getCheckAutoSpreadAllSlavesEverySeconds() {
    return checkAutoSpreadAllSlavesEverySeconds;
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

  public long getCheckJobsEveryMillis() {
    return checkJobsEveryMillis;
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

  public long getDebugCuratorCallOverBytes() {
    return debugCuratorCallOverBytes;
  }

  public void setDebugCuratorCallOverBytes(long debugCuratorCallOverBytes) {
    this.debugCuratorCallOverBytes = debugCuratorCallOverBytes;
  }

  public long getPendingDeployHoldTaskDuringDecommissionMillis() {
    return pendingDeployHoldTaskDuringDecommissionMillis;
  }

  public long getCacheForWebForMillis() {
    return cacheForWebForMillis;
  }

  public void setCacheForWebForMillis(long cacheForWebForMillis) {
    this.cacheForWebForMillis = cacheForWebForMillis;
  }

  public void setPendingDeployHoldTaskDuringDecommissionMillis(long pendingDeployHoldTaskDuringDecommissionMillis) {
    this.pendingDeployHoldTaskDuringDecommissionMillis = pendingDeployHoldTaskDuringDecommissionMillis;
  }

  public long getDebugCuratorCallOverMillis() {
    return debugCuratorCallOverMillis;
  }

  public void setDebugCuratorCallOverMillis(long debugCuratorCallOverMillis) {
    this.debugCuratorCallOverMillis = debugCuratorCallOverMillis;
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

  public int getCacheTasksMaxSize() {
    return cacheTasksMaxSize;
  }

  public void setCacheTasksMaxSize(int cacheTasksMaxSize) {
    this.cacheTasksMaxSize = cacheTasksMaxSize;
  }

  public int getCacheTasksInitialSize() {
    return cacheTasksInitialSize;
  }

  public void setCacheTasksInitialSize(int cacheTasksInitialSize) {
    this.cacheTasksInitialSize = cacheTasksInitialSize;
  }

  public int getCacheDeploysMaxSize() {
    return cacheDeploysMaxSize;
  }

  public void setCacheDeploysMaxSize(int cacheDeploysMaxSize) {
    this.cacheDeploysMaxSize = cacheDeploysMaxSize;
  }

  public int getCacheDeploysInitialSize() {
    return cacheDeploysInitialSize;
  }

  public void setCacheDeploysInitialSize(int cacheDeploysInitialSize) {
    this.cacheDeploysInitialSize = cacheDeploysInitialSize;
  }

  public long getCacheDeploysForMillis() {
    return cacheDeploysForMillis;
  }

  public void setCacheDeploysForMillis(long cacheDeploysForMillis) {
    this.cacheDeploysForMillis = cacheDeploysForMillis;
  }

  public int getCoreThreadpoolSize() {
    return coreThreadpoolSize;
  }

  public CustomExecutorConfiguration getCustomExecutorConfiguration() {
    return customExecutorConfiguration;
  }

  public Optional<DataSourceFactory> getDatabaseConfiguration() {
    return Optional.fromNullable(databaseConfiguration);
  }

  public int getDefaultBounceExpirationMinutes() {
    return defaultBounceExpirationMinutes;
  }

  public void setDefaultBounceExpirationMinutes(int defaultBounceExpirationMinutes) {
    this.defaultBounceExpirationMinutes = defaultBounceExpirationMinutes;
  }

  public SlavePlacement getDefaultSlavePlacement() {
    return defaultSlavePlacement;
  }

  public double getPlacementLeniency() {
    return placementLeniency;
  }

  public int getDefaultDeployStepWaitTimeMs() {
    return defaultDeployStepWaitTimeMs;
  }

  public void setDefaultDeployStepWaitTimeMs(int defaultDeployStepWaitTimeMs) {
    this.defaultDeployStepWaitTimeMs = defaultDeployStepWaitTimeMs;
  }

  public int getDefaultDeployMaxTaskRetries() {
    return defaultDeployMaxTaskRetries;
  }

  public boolean isLdapCacheEnabled() {
    return ldapCacheEnabled;
  }

  public void setLdapCacheEnabled(boolean ldapCacheEnabled) {
    this.ldapCacheEnabled = ldapCacheEnabled;
  }

  public long getLdapCacheSize() {
    return ldapCacheSize;
  }

  public void setLdapCacheSize(long ldapCacheSize) {
    this.ldapCacheSize = ldapCacheSize;
  }

  public long getLdapCacheExpireMillis() {
    return ldapCacheExpireMillis;
  }

  public void setLdapCacheExpireMillis(long ldapCacheExpireMillis) {
    this.ldapCacheExpireMillis = ldapCacheExpireMillis;
  }

  public void setDefaultDeployMaxTaskRetries(int defaultDeployMaxTaskRetries) {
    this.defaultDeployMaxTaskRetries = defaultDeployMaxTaskRetries;
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

  public int getDeployIdLength() {
    return deployIdLength;
  }

  public int getHealthcheckIntervalSeconds() {
    return healthcheckIntervalSeconds;
  }

  public int getHealthcheckStartThreads() {
    return healthcheckStartThreads;
  }

  public int getHealthcheckTimeoutSeconds() {
    return healthcheckTimeoutSeconds;
  }

  public Optional<Integer> getHealthcheckMaxRetries() {
    return healthcheckMaxRetries;
  }

  public Optional<Integer> getHealthcheckMaxTotalTimeoutSeconds() {
    return healthcheckMaxTotalTimeoutSeconds;
  }

  public long getKillTaskIfNotHealthyAfterSeconds() {
    return killTaskIfNotHealthyAfterSeconds;
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

  public long getLoadBalancerRemovalGracePeriodMillis() {
    return loadBalancerRemovalGracePeriodMillis;
  }

  public void setLoadBalancerRemovalGracePeriodMillis(long loadBalancerRemovalGracePeriodMillis) {
    this.loadBalancerRemovalGracePeriodMillis = loadBalancerRemovalGracePeriodMillis;
  }

  public long getDeleteDeadSlavesAfterHours() {
    return deleteDeadSlavesAfterHours;
  }

  public void setDeleteDeadSlavesAfterHours(long deleteDeadSlavesAfterHours) {
    this.deleteDeadSlavesAfterHours = deleteDeadSlavesAfterHours;
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

  public int getMaxUserIdSize() {
    return maxUserIdSize;
  }

  public int getMaxTasksPerOffer() {
    return maxTasksPerOffer;
  }

  public int getMaxTasksPerOfferPerRequest() {
    return maxTasksPerOfferPerRequest;
  }

  public double getLongRunningUsedCpuWeightForOffer() {
    return longRunningUsedCpuWeightForOffer;
  }

  public double getLongRunningUsedMemWeightForOffer() {
    return longRunningUsedMemWeightForOffer;
  }

  public double getLongRunningUsedDiskWeightForOffer() {
    return longRunningUsedDiskWeightForOffer;
  }

  public double getFreeCpuWeightForOffer() {
    return freeCpuWeightForOffer;
  }

  public double getFreeMemWeightForOffer() {
    return freeMemWeightForOffer;
  }

  public double getFreeDiskWeightForOffer() {
    return freeDiskWeightForOffer;
  }

  public double getDefaultOfferScoreForMissingUsage() {
    return defaultOfferScoreForMissingUsage;
  }

  public long getConsiderNonLongRunningTaskLongRunningAfterRunningForSeconds() {
    return considerNonLongRunningTaskLongRunningAfterRunningForSeconds;
  }

  public double getMaxNonLongRunningUsedResourceWeight() {
    return maxNonLongRunningUsedResourceWeight;
  }
  public MesosConfiguration getMesosConfiguration() {
    return mesosConfiguration;
  }

  public NetworkConfiguration getNetworkConfiguration() {
    return networkConfiguration;
  }

  public int getNewTaskCheckerBaseDelaySeconds() {
    return newTaskCheckerBaseDelaySeconds;
  }

  public long getPersistHistoryEverySeconds() {
    return persistHistoryEverySeconds;
  }

  @JsonIgnore
  public Optional<S3Configuration> getS3ConfigurationOptional() {
    return Optional.fromNullable(s3Configuration);
  }

  public long getSandboxHttpTimeoutMillis() {
    return sandboxHttpTimeoutMillis;
  }

  public long getSaveStateEverySeconds() {
    return saveStateEverySeconds;
  }

  @JsonIgnore
  public Optional<SentryConfiguration> getSentryConfigurationOptional(){
    return Optional.fromNullable(sentryConfiguration);
  }

  @JsonIgnore
  public Optional<SMTPConfiguration> getSmtpConfigurationOptional() {
    return Optional.fromNullable(smtpConfiguration);
  }

  public S3Configuration getS3Configuration() {
    return s3Configuration;
  }

  public SentryConfiguration getSentryConfiguration() {
    return sentryConfiguration;
  }

  public SMTPConfiguration getSmtpConfiguration() {
    return smtpConfiguration;
  }

  public long getStartNewReconcileEverySeconds() {
    return startNewReconcileEverySeconds;
  }

  public long getThreadpoolShutdownDelayInSeconds() {
    return threadpoolShutdownDelayInSeconds;
  }

  public void setThreadpoolShutdownDelayInSeconds(long threadpoolShutdownDelayInSeconds) {
    this.threadpoolShutdownDelayInSeconds = threadpoolShutdownDelayInSeconds;
  }

  public UIConfiguration getUiConfiguration() {
    return uiConfiguration;
  }

  public long getCheckQueuedMailsEveryMillis() {
    return checkQueuedMailsEveryMillis;
  }

  public void setCheckQueuedMailsEveryMillis(long checkQueuedMailsEveryMillis) {
    this.checkQueuedMailsEveryMillis = checkQueuedMailsEveryMillis;
  }

  public long getWarnIfScheduledJobIsRunningForAtLeastMillis() {
    return warnIfScheduledJobIsRunningForAtLeastMillis;
  }

  public Optional<Long> getTaskExecutionTimeLimitMillis() {
    return taskExecutionTimeLimitMillis;
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

  public boolean isStoreAllMesosTaskInfoForDebugging() {
    return storeAllMesosTaskInfoForDebugging;
  }

  public void setStoreAllMesosTaskInfoForDebugging(boolean storeAllMesosTaskInfoForDebugging) {
    this.storeAllMesosTaskInfoForDebugging = storeAllMesosTaskInfoForDebugging;
  }

  public boolean isCompressLargeDataObjects() {
    return compressLargeDataObjects;
  }

  public boolean isCreateDeployIds() {
    return createDeployIds;
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

  public void setCheckJobsEveryMillis(long checkJobsEveryMillis) {
    this.checkJobsEveryMillis = checkJobsEveryMillis;
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

  public void setCreateDeployIds(boolean createDeployIds) {
    this.createDeployIds = createDeployIds;
  }

  public void setCustomExecutorConfiguration(CustomExecutorConfiguration customExecutorConfiguration) {
    this.customExecutorConfiguration = customExecutorConfiguration;
  }

  public void setDatabaseConfiguration(DataSourceFactory databaseConfiguration) {
    this.databaseConfiguration = databaseConfiguration;
  }

  public void setDefaultSlavePlacement(SlavePlacement defaultSlavePlacement) {
    this.defaultSlavePlacement = defaultSlavePlacement;
  }

  public void setPlacementLeniency(double placementLeniency) {
    this.placementLeniency = placementLeniency;
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

  public void setDeployIdLength(int deployIdLength) {
    this.deployIdLength = deployIdLength;
  }

  public void setEnableCorsFilter(boolean enableCorsFilter) {
    this.enableCorsFilter = enableCorsFilter;
  }

  public void setHealthcheckIntervalSeconds(int healthcheckIntervalSeconds) {
    this.healthcheckIntervalSeconds = healthcheckIntervalSeconds;
  }

  public void setHealthcheckStartThreads(int healthcheckStartThreads) {
    this.healthcheckStartThreads = healthcheckStartThreads;
  }

  public void setHealthcheckTimeoutSeconds(int healthcheckTimeoutSeconds) {
    this.healthcheckTimeoutSeconds = healthcheckTimeoutSeconds;
  }

  public void setHealthcheckMaxRetries(Optional<Integer> healthcheckMaxRetries) {
    this.healthcheckMaxRetries = healthcheckMaxRetries;
  }

  public void setHealthcheckMaxTotalTimeoutSeconds(Optional<Integer> healthcheckMaxTotalTimeoutSeconds) {
    this.healthcheckMaxTotalTimeoutSeconds = healthcheckMaxTotalTimeoutSeconds;
  }

  public SingularityConfiguration setKillTaskIfNotHealthyAfterSeconds(long killTaskIfNotHealthyAfterSeconds) {
    this.killTaskIfNotHealthyAfterSeconds = killTaskIfNotHealthyAfterSeconds;
    return this;
  }

  public List<Integer> getHealthcheckFailureStatusCodes() {
    return healthcheckFailureStatusCodes;
  }

  public void setHealthcheckFailureStatusCodes(List<Integer> healthcheckFailureStatusCodes) {
    this.healthcheckFailureStatusCodes = healthcheckFailureStatusCodes;
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

  public SingularityConfiguration setMaxUserIdSize(int maxUserIdSize) {
    this.maxUserIdSize = maxUserIdSize;
    return this;
  }

  public void setMaxTasksPerOffer(int maxTasksPerOffer) {
    this.maxTasksPerOffer = maxTasksPerOffer;
  }

  public void setMaxTasksPerOfferPerRequest(int maxTasksPerOfferPerRequest) {
    this.maxTasksPerOfferPerRequest = maxTasksPerOfferPerRequest;
  }

  public SingularityConfiguration setLongRunningUsedCpuWeightForOffer(double longRunningUsedCpuWeightForOffer) {
    this.longRunningUsedCpuWeightForOffer = longRunningUsedCpuWeightForOffer;
    return this;
  }

  public SingularityConfiguration setLongRunningUsedMemWeightForOffer(double longRunningUsedMemWeightForOffer) {
    this.longRunningUsedMemWeightForOffer = longRunningUsedMemWeightForOffer;
    return this;
  }

  public SingularityConfiguration setLongRunningUsedDiskWeightForOffer(double longRunningUsedDiskWeightForOffer) {
    this.longRunningUsedDiskWeightForOffer = longRunningUsedDiskWeightForOffer;
    return this;
  }

  public SingularityConfiguration setFreeCpuWeightForOffer(double freeCpuWeightForOffer) {
    this.freeCpuWeightForOffer = freeCpuWeightForOffer;
    return this;
  }

  public SingularityConfiguration setFreeMemWeightForOffer(double freeMemWeightForOffer) {
    this.freeMemWeightForOffer = freeMemWeightForOffer;
    return this;
  }

  public SingularityConfiguration setFreeDiskWeightForOffer(double freeDiskWeightForOffer) {
    this.freeDiskWeightForOffer = freeDiskWeightForOffer;
    return this;
  }

  public SingularityConfiguration setDefaultOfferScoreForMissingUsage(double defaultOfferScoreForMissingUsage) {
    this.defaultOfferScoreForMissingUsage = defaultOfferScoreForMissingUsage;
    return this;
  }

  public SingularityConfiguration setConsiderNonLongRunningTaskLongRunningAfterRunningForSeconds(long considerNonLongRunningTaskLongRunningAfterRunningForSeconds) {
    this.considerNonLongRunningTaskLongRunningAfterRunningForSeconds = considerNonLongRunningTaskLongRunningAfterRunningForSeconds;
    return this;
  }

  public SingularityConfiguration setMaxNonLongRunningUsedResourceWeight(double maxNonLongRunningUsedResourceWeight) {
    this.maxNonLongRunningUsedResourceWeight = maxNonLongRunningUsedResourceWeight;
    return this;
  }
  public void setMesosConfiguration(MesosConfiguration mesosConfiguration) {
    this.mesosConfiguration = mesosConfiguration;
  }

  public void setNetworkConfiguration(NetworkConfiguration networkConfiguration) {
    this.networkConfiguration = networkConfiguration;
  }

  public void setNewTaskCheckerBaseDelaySeconds(int newTaskCheckerBaseDelaySeconds) {
    this.newTaskCheckerBaseDelaySeconds = newTaskCheckerBaseDelaySeconds;
  }

  public void setDispatchTaskShellCommandsEverySeconds(int dispatchTaskShellCommandsEverySeconds) {
    this.dispatchTaskShellCommandsEverySeconds = dispatchTaskShellCommandsEverySeconds;
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

  public SingularityConfiguration setTaskExecutionTimeLimitMillis(Optional<Long> taskExecutionTimeLimitMillis) {
    this.taskExecutionTimeLimitMillis = taskExecutionTimeLimitMillis;
    return this;
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

  public Optional<Integer> getStartupDelaySeconds() {
    return startupDelaySeconds;
  }

  public void setStartupDelaySeconds(Optional<Integer> startupDelaySeconds) {
    this.startupDelaySeconds = startupDelaySeconds;
  }

  public int getStartupTimeoutSeconds() {
    return startupTimeoutSeconds;
  }

  public void setStartupTimeoutSeconds(int startupTimeoutSeconds) {
    this.startupTimeoutSeconds = startupTimeoutSeconds;
  }

  public int getStartupIntervalSeconds() {
    return startupIntervalSeconds;
  }

  public void setStartupIntervalSeconds(int startupIntervalSeconds) {
    this.startupIntervalSeconds = startupIntervalSeconds;
  }

  public long getReconcileSlavesEveryMinutes() {
    return reconcileSlavesEveryMinutes;
  }

  public void setReconcileSlavesEveryMinutes(long reconcileSlavesEveryMinutes) {
    this.reconcileSlavesEveryMinutes = reconcileSlavesEveryMinutes;
  }

  public long getCacheTasksForMillis() {
    return cacheTasksForMillis;
  }

  public void setCacheTasksForMillis(long cacheTasksForMillis) {
    this.cacheTasksForMillis = cacheTasksForMillis;
  }

  public long getTaskPersistAfterStartupBufferMillis() {
    return taskPersistAfterStartupBufferMillis;
  }

  public void setTaskPersistAfterStartupBufferMillis(long taskPersistAfterStartupBufferMillis) {
    this.taskPersistAfterStartupBufferMillis = taskPersistAfterStartupBufferMillis;
  }

  public LDAPConfiguration getLdapConfiguration() {
    return ldapConfiguration;
  }

  @JsonIgnore
  public Optional<LDAPConfiguration> getLdapConfigurationOptional() {
    return Optional.fromNullable(ldapConfiguration);
  }

  public WebhookAuthConfiguration getWebhookAuthConfiguration() {
    return webhookAuthConfiguration;
  }

  public void setWebhookAuthConfiguration(WebhookAuthConfiguration webhookAuthConfiguration) {
    this.webhookAuthConfiguration = webhookAuthConfiguration;
  }

  public void setLdapConfiguration(LDAPConfiguration ldapConfiguration) {
    this.ldapConfiguration = ldapConfiguration;
  }

  public AuthConfiguration getAuthConfiguration() {
    return authConfiguration;
  }

  public long getCheckExpiringUserActionEveryMillis() {
    return checkExpiringUserActionEveryMillis;
  }

  public void setCheckExpiringUserActionEveryMillis(long checkExpiringUserActionEveryMillis) {
    this.checkExpiringUserActionEveryMillis = checkExpiringUserActionEveryMillis;
  }

  public void setAuthConfiguration(AuthConfiguration authConfiguration) {
    this.authConfiguration = authConfiguration;
  }

  public HistoryPurgingConfiguration getHistoryPurgingConfiguration() {
    return historyPurgingConfiguration;
  }

  public void setHistoryPurgingConfiguration(HistoryPurgingConfiguration historyPurgingConfiguration) {
    this.historyPurgingConfiguration = historyPurgingConfiguration;
  }

  public Map<String, List<String>> getReserveSlavesWithAttributes() {
    return reserveSlavesWithAttributes;
  }

  public void setReserveSlavesWithAttributes(Map<String, List<String>> reserveSlavesWithAttributes) {
    this.reserveSlavesWithAttributes = reserveSlavesWithAttributes;
  }

  public SingularityTaskMetadataConfiguration getTaskMetadataConfiguration() {
    return taskMetadataConfiguration;
  }

  public void setTaskMetadataConfiguration(SingularityTaskMetadataConfiguration taskMetadataConfiguration) {
    this.taskMetadataConfiguration = taskMetadataConfiguration;
  }

  public GraphiteConfiguration getGraphiteConfiguration() {
    return graphiteConfiguration;
  }

  public void setGraphiteConfiguration(GraphiteConfiguration graphiteConfiguration) {
    this.graphiteConfiguration = graphiteConfiguration;
  }

  public boolean isDeleteRemovedRequestsFromLoadBalancer() {
    return deleteRemovedRequestsFromLoadBalancer;
  }

  public void setDeleteRemovedRequestsFromLoadBalancer(boolean deleteRemovedRequestsFromLoadBalancer) {
    this.deleteRemovedRequestsFromLoadBalancer = deleteRemovedRequestsFromLoadBalancer;
  }

  public Optional<Integer> getMaxStaleDeploysPerRequestInZkWhenNoDatabase() {
    return maxStaleDeploysPerRequestInZkWhenNoDatabase;
  }

  public void setMaxStaleDeploysPerRequestInZkWhenNoDatabase(Optional<Integer> maxStaleDeploysPerRequestInZkWhenNoDatabase) {
    this.maxStaleDeploysPerRequestInZkWhenNoDatabase = maxStaleDeploysPerRequestInZkWhenNoDatabase;
  }

  public Optional<Integer> getMaxRequestsWithHistoryInZkWhenNoDatabase() {
    return maxRequestsWithHistoryInZkWhenNoDatabase;
  }

  public void setMaxRequestsWithHistoryInZkWhenNoDatabase(Optional<Integer> maxRequestsWithHistoryInZkWhenNoDatabase) {
    this.maxRequestsWithHistoryInZkWhenNoDatabase = maxRequestsWithHistoryInZkWhenNoDatabase;
  }

  public Optional<Integer> getMaxStaleTasksPerRequestInZkWhenNoDatabase() {
    return maxStaleTasksPerRequestInZkWhenNoDatabase;
  }

  public void setMaxStaleTasksPerRequestInZkWhenNoDatabase(Optional<Integer> maxStaleTasksPerRequestInZkWhenNoDatabase) {
    this.maxStaleTasksPerRequestInZkWhenNoDatabase = maxStaleTasksPerRequestInZkWhenNoDatabase;
  }

  public boolean isTaskHistoryQueryUsesZkFirst() {
    return taskHistoryQueryUsesZkFirst;
  }

  public void setTaskHistoryQueryUsesZkFirst(boolean taskHistoryQueryUsesZkFirst) {
    this.taskHistoryQueryUsesZkFirst = taskHistoryQueryUsesZkFirst;
  }

  public Optional<String> getTaskLabelForLoadBalancerUpstreamGroup() {
    return taskLabelForLoadBalancerUpstreamGroup;
  }

  public void setTaskLabelForLoadBalancerUpstreamGroup(Optional<String> taskLabelForLoadBalancerUpstreamGroup) {
    this.taskLabelForLoadBalancerUpstreamGroup = taskLabelForLoadBalancerUpstreamGroup;
  }

  public DisasterDetectionConfiguration getDisasterDetection() {
    return disasterDetection;
  }

  public void setDisasterDetection(DisasterDetectionConfiguration disasterDetection) {
    this.disasterDetection = disasterDetection;
  }

  public double getDefaultTaskPriorityLevel() {
    return defaultTaskPriorityLevel;
  }

  public void setDefaultTaskPriorityLevel(double defaultTaskPriorityLevel) {
    this.defaultTaskPriorityLevel = defaultTaskPriorityLevel;
  }

  public Map<RequestType, Double> getDefaultTaskPriorityLevelForRequestType() {
    return defaultTaskPriorityLevelForRequestType;
  }

  public void setDefaultTaskPriorityLevelForRequestType(Map<RequestType, Double> defaultTaskPriorityLevelForRequestType) {
    this.defaultTaskPriorityLevelForRequestType = defaultTaskPriorityLevelForRequestType;
  }

  public long getCheckPriorityKillsEveryMillis() {
    return checkPriorityKillsEveryMillis;
  }

  public void setCheckPriorityKillsEveryMillis(long checkPriorityKillsEveryMillis) {
    this.checkPriorityKillsEveryMillis = checkPriorityKillsEveryMillis;
  }

  public double getSchedulerPriorityWeightFactor() {
    return schedulerPriorityWeightFactor;
  }

  public void setSchedulerPriorityWeightFactor(double schedulerPriorityWeightFactor) {
    this.schedulerPriorityWeightFactor = schedulerPriorityWeightFactor;
  }

  public boolean isRebalanceRacksOnScaleDown() {
    return rebalanceRacksOnScaleDown;
  }

  public void setRebalanceRacksOnScaleDown(boolean rebalanceRacksOnScaleDown) {
    this.rebalanceRacksOnScaleDown = rebalanceRacksOnScaleDown;
  }

  public boolean isAllowBounceToSameHost() {
    return allowBounceToSameHost;
  }

  public SingularityConfiguration setAllowBounceToSameHost(boolean allowBounceToSameHost) {
    this.allowBounceToSameHost = allowBounceToSameHost;
    return this;
  }

  public long getCheckUsageEveryMillis() {
    return checkUsageEveryMillis;
  }

  public void setCheckUsageEveryMillis(long checkUsageEveryMillis) {
    this.checkUsageEveryMillis = checkUsageEveryMillis;
  }

  public int getUsageIntervalSeconds() {
    return usageIntervalSeconds;
  }

  public SingularityConfiguration setUsageIntervalSeconds(int usageIntervalSeconds) {
    this.usageIntervalSeconds = usageIntervalSeconds;
    return this;
  }

  public long getCleanUsageEveryMillis() {
    return cleanUsageEveryMillis;
  }

  public void setCleanUsageEveryMillis(long cleanUsageEveryMillis) {
    this.cleanUsageEveryMillis = cleanUsageEveryMillis;
  }

  public int getNumUsageToKeep() {
    return numUsageToKeep;
  }

  public void setNumUsageToKeep(int numUsageToKeep) {
    this.numUsageToKeep = numUsageToKeep;
  }

  public long getCacheOffersForMillis() {
    return cacheOffersForMillis;
  }

  public void setCacheOffersForMillis(long cacheOffersForMillis) {
    this.cacheOffersForMillis = cacheOffersForMillis;
  }

  public int getOfferCacheSize() {
    return offerCacheSize;
  }

  public void setOfferCacheSize(int offerCacheSize) {
    this.offerCacheSize = offerCacheSize;
  }

  public boolean isCacheOffers() {
    return cacheOffers;
  }

  public void setCacheOffers(boolean cacheOffers) {
    this.cacheOffers = cacheOffers;
  }

  public int getMaxActiveOnDemandTasksPerRequest() {
    return maxActiveOnDemandTasksPerRequest;
  }

  public void setMaxActiveOnDemandTasksPerRequest(int maxActiveOnDemandTasksPerRequest) {
    this.maxActiveOnDemandTasksPerRequest = maxActiveOnDemandTasksPerRequest;
  }

  public int getMaxDecommissioningSlaves() {
    return maxDecommissioningSlaves;
  }

  public void setMaxDecommissioningSlaves(int maxDecommissioningSlaves) {
    this.maxDecommissioningSlaves = maxDecommissioningSlaves;
  }

  public boolean isSpreadAllSlavesEnabled() {
    return spreadAllSlavesEnabled;
  }

  public void setSpreadAllSlavesEnabled(boolean spreadAllSlavesEnabled) {
    this.spreadAllSlavesEnabled = spreadAllSlavesEnabled;
  }

  public void setCheckAutoSpreadAllSlavesEverySeconds(long checkAutoSpreadAllSlavesEverySeconds) {
    this.checkAutoSpreadAllSlavesEverySeconds = checkAutoSpreadAllSlavesEverySeconds;
  }

  public long getDelayPollersWhenDeltaOverMs() {
    return delayPollersWhenDeltaOverMs;
  }

  public void setDelayPollersWhenDeltaOverMs(long delayPollersWhenDeltaOverMs) {
    this.delayPollersWhenDeltaOverMs = delayPollersWhenDeltaOverMs;
  }

  public boolean isDelayOfferProcessingForLargeStatusUpdateDelta() {
    return delayOfferProcessingForLargeStatusUpdateDelta;
  }

  public void setDelayOfferProcessingForLargeStatusUpdateDelta(boolean delayOfferProcessingForLargeStatusUpdateDelta) {
    this.delayOfferProcessingForLargeStatusUpdateDelta = delayOfferProcessingForLargeStatusUpdateDelta;
  }

  public int getMaxRunNowTaskLaunchDelayDays() {
    return maxRunNowTaskLaunchDelayDays;
  }

  public void setMaxRunNowTaskLaunchDelayDays(int maxRunNowTaskLaunchDelayDays) {
    this.maxRunNowTaskLaunchDelayDays = maxRunNowTaskLaunchDelayDays;
  }
}
