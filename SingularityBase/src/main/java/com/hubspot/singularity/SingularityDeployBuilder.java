package com.hubspot.singularity;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.hubspot.deploy.ExecutorData;
import com.hubspot.deploy.HealthcheckOptions;
import com.hubspot.mesos.Resources;
import com.hubspot.mesos.SingularityContainerInfo;
import com.hubspot.mesos.SingularityMesosArtifact;
import com.hubspot.mesos.SingularityMesosTaskLabel;
import com.hubspot.singularity.api.SingularityRunNowRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SingularityDeployBuilder {
  private String requestId;

  private String id;

  private Optional<String> version;
  private Optional<Long> timestamp;
  private Optional<Map<String, String>> metadata;

  private Optional<SingularityContainerInfo> containerInfo;

  private Optional<String> customExecutorCmd;
  private Optional<String> customExecutorId;
  private Optional<String> customExecutorSource;
  private Optional<Resources> customExecutorResources;

  private Optional<Resources> resources;

  private Optional<String> command;
  private Optional<List<String>> arguments;
  private Optional<Map<String, String>> env;
  private Optional<Map<Integer, Map<String, String>>> taskEnv;
  private Optional<SingularityRunNowRequest> runImmediately;
  private Optional<List<SingularityMesosArtifact>> uris;
  private Optional<ExecutorData> executorData;
  private Optional<Map<String, String>> labels;
  private Optional<List<SingularityMesosTaskLabel>> mesosLabels;
  private Optional<Map<Integer, Map<String, String>>> taskLabels;
  private Optional<Map<Integer, List<SingularityMesosTaskLabel>>> mesosTaskLabels;

  /**
   * @deprecated use {@link #healthcheck}
   */
  @Deprecated
  private Optional<String> healthcheckUri;

  /**
   * @deprecated use {@link #healthcheck}
   */
  @Deprecated
  private Optional<Long> healthcheckIntervalSeconds;

  /**
   * @deprecated use {@link #healthcheck}
   */
  @Deprecated
  private Optional<Long> healthcheckTimeoutSeconds;

  /**
   * @deprecated use {@link #healthcheck}
   */
  @Deprecated
  private Optional<Integer> healthcheckPortIndex;

  /**
   * @deprecated use {@link #healthcheck}
   */
  @Deprecated
  private Optional<HealthcheckProtocol> healthcheckProtocol;

  /**
   * @deprecated use {@link #healthcheck}
   */
  @Deprecated
  private Optional<Integer> healthcheckMaxRetries;

  /**
   * @deprecated use {@link #healthcheck}
   */
  @Deprecated
  private Optional<Long> healthcheckMaxTotalTimeoutSeconds;

  private Optional<Boolean> skipHealthchecksOnDeploy;

  private Optional<HealthcheckOptions> healthcheck;

  private Optional<Long> deployHealthTimeoutSeconds;

  private Optional<Long> considerHealthyAfterRunningForSeconds;

  private Optional<String> serviceBasePath;
  private Optional<Set<String>> loadBalancerGroups;
  private Optional<Integer> loadBalancerPortIndex;
  private Optional<Map<String, Object>> loadBalancerOptions;
  private Optional<Set<String>> loadBalancerDomains;
  private Optional<List<String>> loadBalancerAdditionalRoutes;
  private Optional<String> loadBalancerTemplate;
  private Optional<String> loadBalancerServiceIdOverride;
  private Optional<String> loadBalancerUpstreamGroup;

  @Deprecated
  /**
   * @deprecated use {@link CanaryDeploySettings} instead
   */
  private Optional<Integer> deployInstanceCountPerStep;

  @Deprecated
  /**
   * @deprecated use {@link CanaryDeploySettings} instead
   */
  private Optional<Integer> deployStepWaitTimeMs;

  @Deprecated
  /**
   * @deprecated use {@link CanaryDeploySettings} instead
   */
  private Optional<Boolean> autoAdvanceDeploySteps;

  @Deprecated
  /**
   * @deprecated use {@link CanaryDeploySettings} instead
   */
  private Optional<Integer> maxTaskRetries;

  private Optional<Boolean> shell;
  private Optional<String> user;
  private List<SingularityS3UploaderFile> s3UploaderAdditionalFiles;
  private Optional<CanaryDeploySettings> canaryDeploySettings;

  public SingularityDeployBuilder(String requestId, String id) {
    this.requestId = requestId;
    this.id = id;
    this.version = Optional.empty();
    this.timestamp = Optional.empty();
    this.metadata = Optional.empty();
    this.containerInfo = Optional.empty();
    this.customExecutorCmd = Optional.empty();
    this.customExecutorId = Optional.empty();
    this.customExecutorSource = Optional.empty();
    this.customExecutorResources = Optional.empty();
    this.resources = Optional.empty();
    this.command = Optional.empty();
    this.arguments = Optional.empty();
    this.env = Optional.empty();
    this.taskEnv = Optional.empty();
    this.runImmediately = Optional.empty();
    this.uris = Optional.empty();
    this.executorData = Optional.empty();
    this.labels = Optional.empty();
    this.mesosLabels = Optional.empty();
    this.taskLabels = Optional.empty();
    this.mesosTaskLabels = Optional.empty();
    this.healthcheckUri = Optional.empty();
    this.healthcheckIntervalSeconds = Optional.empty();
    this.healthcheckTimeoutSeconds = Optional.empty();
    this.healthcheckPortIndex = Optional.empty();
    this.skipHealthchecksOnDeploy = Optional.empty();
    this.deployHealthTimeoutSeconds = Optional.empty();
    this.healthcheck = Optional.empty();
    this.healthcheckProtocol = Optional.empty();
    this.healthcheckMaxTotalTimeoutSeconds = Optional.empty();
    this.healthcheckMaxRetries = Optional.empty();
    this.considerHealthyAfterRunningForSeconds = Optional.empty();
    this.serviceBasePath = Optional.empty();
    this.loadBalancerGroups = Optional.empty();
    this.loadBalancerPortIndex = Optional.empty();
    this.loadBalancerOptions = Optional.empty();
    this.loadBalancerDomains = Optional.empty();
    this.loadBalancerAdditionalRoutes = Optional.empty();
    this.loadBalancerTemplate = Optional.empty();
    this.loadBalancerServiceIdOverride = Optional.empty();
    this.loadBalancerUpstreamGroup = Optional.empty();
    this.deployInstanceCountPerStep = Optional.empty();
    this.deployStepWaitTimeMs = Optional.empty();
    this.autoAdvanceDeploySteps = Optional.empty();
    this.maxTaskRetries = Optional.empty();
    this.shell = Optional.empty();
    this.user = Optional.empty();
    this.s3UploaderAdditionalFiles = Collections.emptyList();
    this.canaryDeploySettings = Optional.empty();
  }

  public SingularityDeploy build() {
    return new SingularityDeploy(
      requestId,
      id,
      command,
      arguments,
      containerInfo,
      customExecutorCmd,
      customExecutorId,
      customExecutorSource,
      customExecutorResources,
      resources,
      env,
      taskEnv,
      runImmediately,
      uris,
      metadata,
      executorData,
      version,
      timestamp,
      labels,
      mesosLabels,
      taskLabels,
      mesosTaskLabels,
      deployHealthTimeoutSeconds,
      healthcheckUri,
      healthcheckIntervalSeconds,
      healthcheckTimeoutSeconds,
      healthcheckPortIndex,
      healthcheckMaxRetries,
      healthcheckMaxTotalTimeoutSeconds,
      healthcheck,
      serviceBasePath,
      loadBalancerGroups,
      loadBalancerPortIndex,
      considerHealthyAfterRunningForSeconds,
      loadBalancerOptions,
      loadBalancerDomains,
      loadBalancerAdditionalRoutes,
      loadBalancerTemplate,
      loadBalancerServiceIdOverride,
      loadBalancerUpstreamGroup,
      skipHealthchecksOnDeploy,
      healthcheckProtocol,
      deployInstanceCountPerStep,
      deployStepWaitTimeMs,
      autoAdvanceDeploySteps,
      maxTaskRetries,
      shell,
      user,
      s3UploaderAdditionalFiles,
      canaryDeploySettings
    );
  }

  public String getRequestId() {
    return requestId;
  }

  public SingularityDeployBuilder setRequestId(String requestId) {
    this.requestId = requestId;
    return this;
  }

  public String getId() {
    return id;
  }

  public SingularityDeployBuilder setId(String id) {
    this.id = id;
    return this;
  }

  public Optional<Long> getConsiderHealthyAfterRunningForSeconds() {
    return considerHealthyAfterRunningForSeconds;
  }

  public SingularityDeployBuilder setConsiderHealthyAfterRunningForSeconds(
    Optional<Long> considerHealthyAfterRunningForSeconds
  ) {
    this.considerHealthyAfterRunningForSeconds = considerHealthyAfterRunningForSeconds;
    return this;
  }

  public Optional<String> getVersion() {
    return version;
  }

  public SingularityDeployBuilder setVersion(Optional<String> version) {
    this.version = version;
    return this;
  }

  public Optional<Long> getTimestamp() {
    return timestamp;
  }

  public SingularityDeployBuilder setTimestamp(Optional<Long> timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  public Optional<Map<String, String>> getMetadata() {
    return metadata;
  }

  public SingularityDeployBuilder setMetadata(Optional<Map<String, String>> metadata) {
    this.metadata = metadata;
    return this;
  }

  public Optional<SingularityContainerInfo> getContainerInfo() {
    return containerInfo;
  }

  public SingularityDeployBuilder setContainerInfo(
    Optional<SingularityContainerInfo> containerInfo
  ) {
    this.containerInfo = containerInfo;
    return this;
  }

  public Optional<String> getCustomExecutorCmd() {
    return customExecutorCmd;
  }

  public SingularityDeployBuilder setCustomExecutorCmd(
    Optional<String> customExecutorCmd
  ) {
    this.customExecutorCmd = customExecutorCmd;
    return this;
  }

  public Optional<String> getCustomExecutorId() {
    return customExecutorId;
  }

  public SingularityDeployBuilder setCustomExecutorId(Optional<String> customExecutorId) {
    this.customExecutorId = customExecutorId;
    return this;
  }

  public Optional<String> getCustomExecutorSource() {
    return customExecutorSource;
  }

  public SingularityDeployBuilder setCustomExecutorSource(
    Optional<String> customExecutorSource
  ) {
    this.customExecutorSource = customExecutorSource;
    return this;
  }

  public Optional<Resources> getCustomExecutorResources() {
    return customExecutorResources;
  }

  public SingularityDeployBuilder setCustomExecutorResources(
    Optional<Resources> customExecutorResources
  ) {
    this.customExecutorResources = customExecutorResources;
    return this;
  }

  public Optional<Long> getDeployHealthTimeoutSeconds() {
    return deployHealthTimeoutSeconds;
  }

  public SingularityDeployBuilder setDeployHealthTimeoutSeconds(
    Optional<Long> deployHealthTimeoutSeconds
  ) {
    this.deployHealthTimeoutSeconds = deployHealthTimeoutSeconds;
    return this;
  }

  public Optional<Resources> getResources() {
    return resources;
  }

  public SingularityDeployBuilder setResources(Optional<Resources> resources) {
    this.resources = resources;
    return this;
  }

  public Optional<String> getCommand() {
    return command;
  }

  public SingularityDeployBuilder setCommand(Optional<String> command) {
    this.command = command;
    return this;
  }

  public Optional<List<String>> getArguments() {
    return arguments;
  }

  public SingularityDeployBuilder setArguments(Optional<List<String>> arguments) {
    this.arguments = arguments;
    return this;
  }

  public Optional<Map<String, String>> getEnv() {
    return env;
  }

  public SingularityDeployBuilder setEnv(Optional<Map<String, String>> env) {
    this.env = env;
    return this;
  }

  public Optional<Map<Integer, Map<String, String>>> getTaskEnv() {
    return taskEnv;
  }

  public SingularityDeployBuilder setTaskEnv(
    Optional<Map<Integer, Map<String, String>>> taskEnv
  ) {
    this.taskEnv = taskEnv;
    return this;
  }

  public Optional<SingularityRunNowRequest> getRunImmediately() {
    return runImmediately;
  }

  public SingularityDeployBuilder setRunImmediately(
    Optional<SingularityRunNowRequest> runImmediately
  ) {
    this.runImmediately = runImmediately;
    return this;
  }

  public Optional<List<SingularityMesosArtifact>> getUris() {
    return uris;
  }

  public SingularityDeployBuilder setUris(Optional<List<SingularityMesosArtifact>> uris) {
    this.uris = uris;
    return this;
  }

  public Optional<ExecutorData> getExecutorData() {
    return executorData;
  }

  public SingularityDeployBuilder setExecutorData(Optional<ExecutorData> executorData) {
    this.executorData = executorData;
    return this;
  }

  public Optional<String> getHealthcheckUri() {
    return healthcheckUri;
  }

  public SingularityDeployBuilder setHealthcheckUri(Optional<String> healthcheckUri) {
    this.healthcheckUri = healthcheckUri;
    return this;
  }

  public Optional<Long> getHealthcheckIntervalSeconds() {
    return healthcheckIntervalSeconds;
  }

  public SingularityDeployBuilder setHealthcheckIntervalSeconds(
    Optional<Long> healthcheckIntervalSeconds
  ) {
    this.healthcheckIntervalSeconds = healthcheckIntervalSeconds;
    return this;
  }

  public Optional<Long> getHealthcheckTimeoutSeconds() {
    return healthcheckTimeoutSeconds;
  }

  public SingularityDeployBuilder setHealthcheckTimeoutSeconds(
    Optional<Long> healthcheckTimeoutSeconds
  ) {
    this.healthcheckTimeoutSeconds = healthcheckTimeoutSeconds;
    return this;
  }

  public Optional<Integer> getHealthcheckPortIndex() {
    return healthcheckPortIndex;
  }

  public SingularityDeployBuilder setHealthcheckPortIndex(
    Optional<Integer> healthcheckPortIndex
  ) {
    this.healthcheckPortIndex = healthcheckPortIndex;
    return this;
  }

  public Optional<String> getServiceBasePath() {
    return serviceBasePath;
  }

  public SingularityDeployBuilder setServiceBasePath(Optional<String> serviceBasePath) {
    this.serviceBasePath = serviceBasePath;
    return this;
  }

  public Optional<Set<String>> getLoadBalancerGroups() {
    return loadBalancerGroups;
  }

  public SingularityDeployBuilder setLoadBalancerGroups(
    Optional<Set<String>> loadBalancerGroups
  ) {
    this.loadBalancerGroups = loadBalancerGroups;
    return this;
  }

  public Optional<Integer> getLoadBalancerPortIndex() {
    return loadBalancerPortIndex;
  }

  public SingularityDeployBuilder setLoadBalancerPortIndex(
    Optional<Integer> loadBalancerPortIndex
  ) {
    this.loadBalancerPortIndex = loadBalancerPortIndex;
    return this;
  }

  public Optional<Map<String, Object>> getLoadBalancerOptions() {
    return loadBalancerOptions;
  }

  public SingularityDeployBuilder setLoadBalancerOptions(
    Optional<Map<String, Object>> loadBalancerOptions
  ) {
    this.loadBalancerOptions = loadBalancerOptions;
    return this;
  }

  public Optional<Set<String>> getLoadBalancerDomains() {
    return loadBalancerDomains;
  }

  public SingularityDeployBuilder setLoadBalancerDomains(
    Optional<Set<String>> loadBalancerDomains
  ) {
    this.loadBalancerDomains = loadBalancerDomains;
    return this;
  }

  public Optional<List<String>> getLoadBalancerAdditionalRoutes() {
    return loadBalancerAdditionalRoutes;
  }

  public SingularityDeployBuilder setLoadBalancerAdditionalRoutes(
    Optional<List<String>> loadBalancerAdditionalRoutes
  ) {
    this.loadBalancerAdditionalRoutes = loadBalancerAdditionalRoutes;
    return this;
  }

  public Optional<String> getLoadBalancerTemplate() {
    return loadBalancerTemplate;
  }

  public SingularityDeployBuilder setLoadBalancerTemplate(
    Optional<String> loadBalancerTemplate
  ) {
    this.loadBalancerTemplate = loadBalancerTemplate;
    return this;
  }

  @Deprecated
  public Optional<Map<String, String>> getLabels() {
    return labels;
  }

  @Deprecated
  public SingularityDeployBuilder setLabels(Optional<Map<String, String>> labels) {
    this.labels = labels;
    return this;
  }

  public Optional<List<SingularityMesosTaskLabel>> getMesosLabels() {
    return mesosLabels;
  }

  public SingularityDeployBuilder setMesosLabels(
    Optional<List<SingularityMesosTaskLabel>> mesosLabels
  ) {
    this.mesosLabels = mesosLabels;
    return this;
  }

  @Deprecated
  public Optional<Map<Integer, Map<String, String>>> getTaskLabels() {
    return taskLabels;
  }

  @Deprecated
  public SingularityDeployBuilder setTaskLabels(
    Optional<Map<Integer, Map<String, String>>> taskLabels
  ) {
    this.taskLabels = taskLabels;
    return this;
  }

  public Optional<Map<Integer, List<SingularityMesosTaskLabel>>> getMesosTaskLabels() {
    return mesosTaskLabels;
  }

  public SingularityDeployBuilder setMesosTaskLabels(
    Optional<Map<Integer, List<SingularityMesosTaskLabel>>> mesosTaskLabels
  ) {
    this.mesosTaskLabels = mesosTaskLabels;
    return this;
  }

  public Optional<Boolean> getSkipHealthchecksOnDeploy() {
    return skipHealthchecksOnDeploy;
  }

  public SingularityDeployBuilder setSkipHealthchecksOnDeploy(
    Optional<Boolean> skipHealthchecksOnDeploy
  ) {
    this.skipHealthchecksOnDeploy = skipHealthchecksOnDeploy;
    return this;
  }

  public Optional<HealthcheckProtocol> getHealthcheckProtocol() {
    return healthcheckProtocol;
  }

  public SingularityDeployBuilder setHealthcheckProtocol(
    Optional<HealthcheckProtocol> healthcheckProtocol
  ) {
    this.healthcheckProtocol = healthcheckProtocol;
    return this;
  }

  public Optional<Integer> getHealthcheckMaxRetries() {
    return healthcheckMaxRetries;
  }

  public Optional<Long> getHealthcheckMaxTotalTimeoutSeconds() {
    return healthcheckMaxTotalTimeoutSeconds;
  }

  public SingularityDeployBuilder setHealthcheckMaxRetries(
    Optional<Integer> healthcheckMaxRetries
  ) {
    this.healthcheckMaxRetries = healthcheckMaxRetries;
    return this;
  }

  public SingularityDeployBuilder setHealthcheckMaxTotalTimeoutSeconds(
    Optional<Long> healthcheckMaxTotalTimeoutSeconds
  ) {
    this.healthcheckMaxTotalTimeoutSeconds = healthcheckMaxTotalTimeoutSeconds;
    return this;
  }

  public Optional<HealthcheckOptions> getHealthcheck() {
    return healthcheck;
  }

  public SingularityDeployBuilder setHealthcheck(
    Optional<HealthcheckOptions> healthcheck
  ) {
    this.healthcheck = healthcheck;
    return this;
  }

  @Deprecated
  /**
   * @deprecated use {@link CanaryDeploySettings} instead
   */
  public Optional<Integer> getDeployInstanceCountPerStep() {
    return deployInstanceCountPerStep;
  }

  @Deprecated
  /**
   * @deprecated use {@link CanaryDeploySettings} instead
   */
  public SingularityDeployBuilder setDeployInstanceCountPerStep(
    Optional<Integer> deployInstanceCountPerStep
  ) {
    this.deployInstanceCountPerStep = deployInstanceCountPerStep;
    return this;
  }

  @Deprecated
  /**
   * @deprecated use {@link CanaryDeploySettings} instead
   */
  public Optional<Integer> getDeployStepWaitTimeMs() {
    return deployStepWaitTimeMs;
  }

  @Deprecated
  /**
   * @deprecated use {@link CanaryDeploySettings} instead
   */
  public SingularityDeployBuilder setDeployStepWaitTimeMs(
    Optional<Integer> deployStepWaitTimeMs
  ) {
    this.deployStepWaitTimeMs = deployStepWaitTimeMs;
    return this;
  }

  @Deprecated
  /**
   * @deprecated use {@link CanaryDeploySettings} instead
   */
  public Optional<Boolean> getAutoAdvanceDeploySteps() {
    return autoAdvanceDeploySteps;
  }

  @Deprecated
  /**
   * @deprecated use {@link CanaryDeploySettings} instead
   */
  public SingularityDeployBuilder setAutoAdvanceDeploySteps(
    Optional<Boolean> autoAdvanceDeploySteps
  ) {
    this.autoAdvanceDeploySteps = autoAdvanceDeploySteps;
    return this;
  }

  @Deprecated
  /**
   * @deprecated use {@link CanaryDeploySettings} instead
   */
  public Optional<Integer> getMaxTaskRetries() {
    return maxTaskRetries;
  }

  @Deprecated
  /**
   * @deprecated use {@link CanaryDeploySettings} instead
   */
  public SingularityDeployBuilder setMaxTaskRetries(Optional<Integer> maxTaskRetries) {
    this.maxTaskRetries = maxTaskRetries;
    return this;
  }

  public Optional<Boolean> getShell() {
    return shell;
  }

  public SingularityDeployBuilder setShell(Optional<Boolean> shell) {
    this.shell = shell;
    return this;
  }

  public Optional<String> getUser() {
    return user;
  }

  public SingularityDeployBuilder setUser(Optional<String> user) {
    this.user = user;
    return this;
  }

  public Optional<String> getLoadBalancerServiceIdOverride() {
    return loadBalancerServiceIdOverride;
  }

  public SingularityDeployBuilder setLoadBalancerServiceIdOverride(
    Optional<String> loadBalancerServiceIdOverride
  ) {
    this.loadBalancerServiceIdOverride = loadBalancerServiceIdOverride;
    return this;
  }

  public Optional<String> getLoadBalancerUpstreamGroup() {
    return loadBalancerUpstreamGroup;
  }

  public SingularityDeployBuilder setLoadBalancerUpstreamGroup(
    Optional<String> loadBalancerUpstreamGroup
  ) {
    this.loadBalancerUpstreamGroup = loadBalancerUpstreamGroup;
    return this;
  }

  public List<SingularityS3UploaderFile> getS3UploaderAdditionalFiles() {
    return s3UploaderAdditionalFiles;
  }

  public SingularityDeployBuilder setS3UploaderAdditionalFiles(
    List<SingularityS3UploaderFile> s3UploaderAdditionalFiles
  ) {
    this.s3UploaderAdditionalFiles = s3UploaderAdditionalFiles;
    return this;
  }

  public Optional<CanaryDeploySettings> getCanaryDeploySettings() {
    return canaryDeploySettings;
  }

  public SingularityDeployBuilder setCanaryDeploySettings(
    CanaryDeploySettings canaryDeploySettings
  ) {
    this.canaryDeploySettings = Optional.ofNullable(canaryDeploySettings);
    return this;
  }

  @Override
  public String toString() {
    return MoreObjects
      .toStringHelper(this)
      .add("requestId", requestId)
      .add("id", id)
      .add("version", version)
      .add("timestamp", timestamp)
      .add("metadata", metadata)
      .add("containerInfo", containerInfo)
      .add("customExecutorCmd", customExecutorCmd)
      .add("customExecutorId", customExecutorId)
      .add("customExecutorSource", customExecutorSource)
      .add("customExecutorResources", customExecutorResources)
      .add("resources", resources)
      .add("command", command)
      .add("arguments", arguments)
      .add("env", env)
      .add("taskEnv", taskEnv)
      .add("runImmediately", runImmediately)
      .add("uris", uris)
      .add("executorData", executorData)
      .add("labels", labels)
      .add("mesosLabels", mesosLabels)
      .add("taskLabels", taskLabels)
      .add("mesosTaskLabels", mesosTaskLabels)
      .add("healthcheckUri", healthcheckUri)
      .add("healthcheckIntervalSeconds", healthcheckIntervalSeconds)
      .add("healthcheckTimeoutSeconds", healthcheckTimeoutSeconds)
      .add("healthcheckPortIndex", healthcheckPortIndex)
      .add("healthcheckProtocol", healthcheckProtocol)
      .add("healthcheckMaxRetries", healthcheckMaxRetries)
      .add("healthcheckMaxTotalTimeoutSeconds", healthcheckMaxTotalTimeoutSeconds)
      .add("skipHealthchecksOnDeploy", skipHealthchecksOnDeploy)
      .add("healthcheck", healthcheck)
      .add("deployHealthTimeoutSeconds", deployHealthTimeoutSeconds)
      .add("considerHealthyAfterRunningForSeconds", considerHealthyAfterRunningForSeconds)
      .add("serviceBasePath", serviceBasePath)
      .add("loadBalancerGroups", loadBalancerGroups)
      .add("loadBalancerPortIndex", loadBalancerPortIndex)
      .add("loadBalancerOptions", loadBalancerOptions)
      .add("loadBalancerDomains", loadBalancerDomains)
      .add("loadBalancerAdditionalRoutes", loadBalancerAdditionalRoutes)
      .add("loadBalancerTemplate", loadBalancerTemplate)
      .add("loadBalancerServiceIdOverride", loadBalancerServiceIdOverride)
      .add("loadBalancerUpstreamGroup", loadBalancerUpstreamGroup)
      .add("deployInstanceCountPerStep", deployInstanceCountPerStep)
      .add("deployStepWaitTimeMs", deployStepWaitTimeMs)
      .add("autoAdvanceDeploySteps", autoAdvanceDeploySteps)
      .add("maxTaskRetries", maxTaskRetries)
      .add("shell", shell)
      .add("user", user)
      .add("s3UploaderAdditionalFiles", s3UploaderAdditionalFiles)
      .add("canaryDeploySettings", canaryDeploySettings)
      .toString();
  }
}
