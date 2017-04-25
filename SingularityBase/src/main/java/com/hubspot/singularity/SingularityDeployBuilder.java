package com.hubspot.singularity;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Optional;
import com.hubspot.deploy.ExecutorData;
import com.hubspot.deploy.HealthcheckOptions;
import com.hubspot.mesos.Resources;
import com.hubspot.mesos.SingularityContainerInfo;
import com.hubspot.mesos.SingularityMesosArtifact;
import com.hubspot.mesos.SingularityMesosTaskLabel;
import com.hubspot.singularity.api.SingularityRunNowRequest;

public class SingularityDeployBuilder {

  private final String requestId;

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

  private Optional<Integer> deployInstanceCountPerStep;
  private Optional<Integer> deployStepWaitTimeMs;
  private Optional<Boolean> autoAdvanceDeploySteps;
  private Optional<Integer> maxTaskRetries;
  private Optional<Boolean> shell;
  private Optional<String> user;

  public SingularityDeployBuilder(String requestId, String id) {
    this.requestId = requestId;
    this.id = id;
    this.version = Optional.absent();
    this.timestamp = Optional.absent();
    this.metadata = Optional.absent();
    this.containerInfo = Optional.absent();
    this.customExecutorCmd = Optional.absent();
    this.customExecutorId = Optional.absent();
    this.customExecutorSource = Optional.absent();
    this.customExecutorResources = Optional.absent();
    this.resources = Optional.absent();
    this.command = Optional.absent();
    this.arguments = Optional.absent();
    this.env = Optional.absent();
    this.taskEnv = Optional.absent();
    this.runImmediately = Optional.absent();
    this.uris = Optional.absent();
    this.executorData = Optional.absent();
    this.labels = Optional.absent();
    this.mesosLabels = Optional.absent();
    this.taskLabels = Optional.absent();
    this.mesosTaskLabels = Optional.absent();
    this.healthcheckUri = Optional.absent();
    this.healthcheckIntervalSeconds = Optional.absent();
    this.healthcheckTimeoutSeconds = Optional.absent();
    this.healthcheckPortIndex = Optional.absent();
    this.skipHealthchecksOnDeploy = Optional.absent();
    this.deployHealthTimeoutSeconds = Optional.absent();
    this.healthcheck = Optional.absent();
    this.healthcheckProtocol = Optional.absent();
    this.healthcheckMaxTotalTimeoutSeconds = Optional.absent();
    this.healthcheckMaxRetries = Optional.absent();
    this.considerHealthyAfterRunningForSeconds = Optional.absent();
    this.serviceBasePath = Optional.absent();
    this.loadBalancerGroups = Optional.absent();
    this.loadBalancerPortIndex = Optional.absent();
    this.loadBalancerOptions = Optional.absent();
    this.loadBalancerDomains = Optional.absent();
    this.loadBalancerAdditionalRoutes = Optional.absent();
    this.loadBalancerTemplate = Optional.absent();
    this.loadBalancerServiceIdOverride = Optional.absent();
    this.loadBalancerUpstreamGroup = Optional.absent();
    this.deployInstanceCountPerStep = Optional.absent();
    this.deployStepWaitTimeMs = Optional.absent();
    this.autoAdvanceDeploySteps = Optional.absent();
    this.maxTaskRetries = Optional.absent();
    this.shell = Optional.absent();
    this.user = Optional.absent();
  }

  public SingularityDeploy build() {
    return new SingularityDeploy(requestId, id, command, arguments, containerInfo, customExecutorCmd, customExecutorId, customExecutorSource, customExecutorResources, resources,
      env, taskEnv, runImmediately, uris, metadata, executorData, version, timestamp, labels, mesosLabels, taskLabels, mesosTaskLabels, deployHealthTimeoutSeconds, healthcheckUri, healthcheckIntervalSeconds, healthcheckTimeoutSeconds, healthcheckPortIndex, healthcheckMaxRetries,
      healthcheckMaxTotalTimeoutSeconds, healthcheck, serviceBasePath, loadBalancerGroups, loadBalancerPortIndex, considerHealthyAfterRunningForSeconds, loadBalancerOptions, loadBalancerDomains, loadBalancerAdditionalRoutes,
      loadBalancerTemplate, loadBalancerServiceIdOverride, loadBalancerUpstreamGroup, skipHealthchecksOnDeploy, healthcheckProtocol, deployInstanceCountPerStep, deployStepWaitTimeMs, autoAdvanceDeploySteps, maxTaskRetries, shell, user);
  }

  public String getRequestId() {
    return requestId;
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

  public SingularityDeployBuilder setConsiderHealthyAfterRunningForSeconds(Optional<Long> considerHealthyAfterRunningForSeconds) {
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

  public SingularityDeployBuilder setContainerInfo(Optional<SingularityContainerInfo> containerInfo) {
    this.containerInfo = containerInfo;
    return this;
  }

  public Optional<String> getCustomExecutorCmd() {
    return customExecutorCmd;
  }

  public SingularityDeployBuilder setCustomExecutorCmd(Optional<String> customExecutorCmd) {
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

  public SingularityDeployBuilder setCustomExecutorSource(Optional<String> customExecutorSource) {
    this.customExecutorSource = customExecutorSource;
    return this;
  }

  public Optional<Resources> getCustomExecutorResources() {
    return customExecutorResources;
  }

  public SingularityDeployBuilder setCustomExecutorResources(Optional<Resources> customExecutorResources) {
    this.customExecutorResources = customExecutorResources;
    return this;
  }

  public Optional<Long> getDeployHealthTimeoutSeconds() {
    return deployHealthTimeoutSeconds;
  }

  public SingularityDeployBuilder setDeployHealthTimeoutSeconds(Optional<Long> deployHealthTimeoutSeconds) {
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

  public SingularityDeployBuilder setTaskEnv(Optional<Map<Integer, Map<String, String>>> taskEnv) {
    this.taskEnv = taskEnv;
    return this;
  }

  public Optional<SingularityRunNowRequest> getRunImmediately() {
    return runImmediately;
  }

  public SingularityDeployBuilder setRunImmediately(Optional<SingularityRunNowRequest> runImmediately) {
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

  public SingularityDeployBuilder setHealthcheckIntervalSeconds(Optional<Long> healthcheckIntervalSeconds) {
    this.healthcheckIntervalSeconds = healthcheckIntervalSeconds;
    return this;
  }

  public Optional<Long> getHealthcheckTimeoutSeconds() {
    return healthcheckTimeoutSeconds;
  }

  public SingularityDeployBuilder setHealthcheckTimeoutSeconds(Optional<Long> healthcheckTimeoutSeconds) {
    this.healthcheckTimeoutSeconds = healthcheckTimeoutSeconds;
    return this;
  }

  public Optional<Integer> getHealthcheckPortIndex() {
    return healthcheckPortIndex;
  }

  public SingularityDeployBuilder setHealthcheckPortIndex(Optional<Integer> healthcheckPortIndex) {
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

  public SingularityDeployBuilder setLoadBalancerGroups(Optional<Set<String>> loadBalancerGroups) {
    this.loadBalancerGroups = loadBalancerGroups;
    return this;
  }

  public Optional<Integer> getLoadBalancerPortIndex() {
    return loadBalancerPortIndex;
  }

  public SingularityDeployBuilder setLoadBalancerPortIndex(Optional<Integer> loadBalancerPortIndex) {
    this.loadBalancerPortIndex = loadBalancerPortIndex;
    return this;
  }

  public Optional<Map<String, Object>> getLoadBalancerOptions() {
    return loadBalancerOptions;
  }

  public SingularityDeployBuilder setLoadBalancerOptions(Optional<Map<String, Object>> loadBalancerOptions) {
    this.loadBalancerOptions = loadBalancerOptions;
    return this;
  }

  public Optional<Set<String>> getLoadBalancerDomains() {
    return loadBalancerDomains;
  }

  public SingularityDeployBuilder setLoadBalancerDomains(Optional<Set<String>> loadBalancerDomains) {
    this.loadBalancerDomains = loadBalancerDomains;
    return this;
  }

  public Optional<List<String>> getLoadBalancerAdditionalRoutes() {
    return loadBalancerAdditionalRoutes;
  }

  public SingularityDeployBuilder setLoadBalancerAdditionalRoutes(Optional<List<String>> loadBalancerAdditionalRoutes) {
    this.loadBalancerAdditionalRoutes = loadBalancerAdditionalRoutes;
    return this;
  }

  public Optional<String> getLoadBalancerTemplate() {
    return loadBalancerTemplate;
  }

  public SingularityDeployBuilder setLoadBalancerTemplate(Optional<String> loadBalancerTemplate) {
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

  public SingularityDeployBuilder setMesosLabels(Optional<List<SingularityMesosTaskLabel>> mesosLabels) {
    this.mesosLabels = mesosLabels;
    return this;
  }

  @Deprecated
  public Optional<Map<Integer, Map<String, String>>> getTaskLabels() {
    return taskLabels;
  }

  @Deprecated
  public SingularityDeployBuilder setTaskLabels(Optional<Map<Integer, Map<String, String>>> taskLabels) {
    this.taskLabels = taskLabels;
    return this;
  }

  public Optional<Map<Integer, List<SingularityMesosTaskLabel>>> getMesosTaskLabels() {
    return mesosTaskLabels;
  }

  public SingularityDeployBuilder setMesosTaskLabels(Optional<Map<Integer, List<SingularityMesosTaskLabel>>> mesosTaskLabels) {
    this.mesosTaskLabels = mesosTaskLabels;
    return this;
  }

  public Optional<Boolean> getSkipHealthchecksOnDeploy() {
    return skipHealthchecksOnDeploy;
  }

  public SingularityDeployBuilder setSkipHealthchecksOnDeploy(Optional<Boolean> skipHealthchecksOnDeploy) {
    this.skipHealthchecksOnDeploy = skipHealthchecksOnDeploy;
    return this;
  }

  public Optional<HealthcheckProtocol> getHealthcheckProtocol() {
    return healthcheckProtocol;
  }

  public SingularityDeployBuilder setHealthcheckProtocol(Optional<HealthcheckProtocol> healthcheckProtocol) {
    this.healthcheckProtocol = healthcheckProtocol;
    return this;
  }

  public Optional<Integer> getHealthcheckMaxRetries() {
    return healthcheckMaxRetries;
  }

  public Optional<Long> getHealthcheckMaxTotalTimeoutSeconds() {
    return healthcheckMaxTotalTimeoutSeconds;
  }

  public SingularityDeployBuilder setHealthcheckMaxRetries(Optional<Integer> healthcheckMaxRetries) {
    this.healthcheckMaxRetries = healthcheckMaxRetries;
    return this;
  }

  public SingularityDeployBuilder setHealthcheckMaxTotalTimeoutSeconds(Optional<Long> healthcheckMaxTotalTimeoutSeconds) {
    this.healthcheckMaxTotalTimeoutSeconds = healthcheckMaxTotalTimeoutSeconds;
    return this;
  }

  public Optional<HealthcheckOptions> getHealthcheck() {
    return healthcheck;
  }

  public SingularityDeployBuilder setHealthcheck(Optional<HealthcheckOptions> healthcheck) {
    this.healthcheck = healthcheck;
    return this;
  }

  public Optional<Integer> getDeployInstanceCountPerStep() {
    return deployInstanceCountPerStep;
  }

  public SingularityDeployBuilder setDeployInstanceCountPerStep(Optional<Integer> deployInstanceCountPerStep) {
    this.deployInstanceCountPerStep = deployInstanceCountPerStep;
    return this;
  }

  public Optional<Integer> getDeployStepWaitTimeMs() {
    return deployStepWaitTimeMs;
  }

  public SingularityDeployBuilder setDeployStepWaitTimeMs(Optional<Integer> deployStepWaitTimeMs) {
    this.deployStepWaitTimeMs = deployStepWaitTimeMs;
    return this;
  }

  public Optional<Boolean> getAutoAdvanceDeploySteps() {
    return autoAdvanceDeploySteps;
  }

  public SingularityDeployBuilder setAutoAdvanceDeploySteps(Optional<Boolean> autoAdvanceDeploySteps) {
    this.autoAdvanceDeploySteps = autoAdvanceDeploySteps;
    return this;
  }

  public Optional<Integer> getMaxTaskRetries() {
    return maxTaskRetries;
  }

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

  public SingularityDeployBuilder setLoadBalancerServiceIdOverride(Optional<String> loadBalancerServiceIdOverride) {
    this.loadBalancerServiceIdOverride = loadBalancerServiceIdOverride;
    return this;
  }

  public Optional<String> getLoadBalancerUpstreamGroup() {
    return loadBalancerUpstreamGroup;
  }

  public SingularityDeployBuilder setLoadBalancerUpstreamGroup(Optional<String> loadBalancerUpstreamGroup) {
    this.loadBalancerUpstreamGroup = loadBalancerUpstreamGroup;
    return this;
  }

  @Override
  public String toString() {
    return "SingularityDeployBuilder{" +
        "requestId='" + requestId + '\'' +
        ", id='" + id + '\'' +
        ", version=" + version +
        ", timestamp=" + timestamp +
        ", metadata=" + metadata +
        ", containerInfo=" + containerInfo +
        ", customExecutorCmd=" + customExecutorCmd +
        ", customExecutorId=" + customExecutorId +
        ", customExecutorSource=" + customExecutorSource +
        ", customExecutorResources=" + customExecutorResources +
        ", resources=" + resources +
        ", command=" + command +
        ", arguments=" + arguments +
        ", env=" + env +
        ", taskEnv=" + taskEnv +
        ", uris=" + uris +
        ", executorData=" + executorData +
        ", labels=" + labels +
        ", mesosLabels=" + mesosLabels +
        ", taskLabels=" + taskLabels +
        ", mesosTaskLabels=" + mesosTaskLabels +
        ", healthcheckUri=" + healthcheckUri +
        ", healthcheckIntervalSeconds=" + healthcheckIntervalSeconds +
        ", healthcheckTimeoutSeconds=" + healthcheckTimeoutSeconds +
        ", healthcheckPortIndex=" + healthcheckPortIndex +
        ", healthcheckProtocol=" + healthcheckProtocol +
        ", healthcheckMaxRetries=" + healthcheckMaxRetries +
        ", healthcheckMaxTotalTimeoutSeconds=" + healthcheckMaxTotalTimeoutSeconds +
        ", skipHealthchecksOnDeploy=" + skipHealthchecksOnDeploy +
        ", healthcheck=" + healthcheck +
        ", deployHealthTimeoutSeconds=" + deployHealthTimeoutSeconds +
        ", considerHealthyAfterRunningForSeconds=" + considerHealthyAfterRunningForSeconds +
        ", serviceBasePath=" + serviceBasePath +
        ", loadBalancerGroups=" + loadBalancerGroups +
        ", loadBalancerPortIndex=" + loadBalancerPortIndex +
        ", loadBalancerOptions=" + loadBalancerOptions +
        ", loadBalancerDomains=" + loadBalancerDomains +
        ", loadBalancerAdditionalRoutes=" + loadBalancerAdditionalRoutes +
        ", loadBalancerTemplate=" + loadBalancerTemplate +
        ", loadBalancerServiceIdOverride=" + loadBalancerServiceIdOverride +
        ", loadBalancerUpstreamGroup=" + loadBalancerUpstreamGroup +
        ", deployInstanceCountPerStep=" + deployInstanceCountPerStep +
        ", deployStepWaitTimeMs=" + deployStepWaitTimeMs +
        ", autoAdvanceDeploySteps=" + autoAdvanceDeploySteps +
        ", maxTaskRetries=" + maxTaskRetries +
        ", shell=" + shell +
        ", user=" + user +
        '}';
  }
}
