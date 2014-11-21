package com.hubspot.singularity;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.google.common.base.Optional;
import com.hubspot.deploy.ExecutorData;
import com.hubspot.mesos.Resources;
import com.hubspot.mesos.SingularityContainerInfo;
import com.hubspot.mesos.SingularityResourceRequest;

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
  private Optional<Resources> resources;
  private Optional<List<SingularityResourceRequest>> requestedResources;

  private Optional<String> command;
  private Optional<List<String>> arguments;
  private Optional<Map<String, String>> env;
  private Optional<List<String>> uris;
  private Optional<ExecutorData> executorData;

  private Optional<String> healthcheckUri;
  private Optional<Long> healthcheckIntervalSeconds;
  private Optional<Long> healthcheckTimeoutSeconds;
  private Optional<Boolean> skipHealthchecksOnDeploy;

  private Optional<Long> deployHealthTimeoutSeconds;

  private Optional<Long> considerHealthyAfterRunningForSeconds;

  private Optional<String> serviceBasePath;
  private Optional<List<String>> loadBalancerGroups;
  private Optional<Map<String, Object>> loadBalancerOptions;

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
    this.resources = Optional.absent();
    this.requestedResources = Optional.absent();
    this.command = Optional.absent();
    this.arguments = Optional.absent();
    this.env = Optional.absent();
    this.uris = Optional.absent();
    this.executorData = Optional.absent();
    this.healthcheckUri = Optional.absent();
    this.healthcheckIntervalSeconds = Optional.absent();
    this.healthcheckTimeoutSeconds = Optional.absent();
    this.skipHealthchecksOnDeploy = Optional.absent();
    this.deployHealthTimeoutSeconds = Optional.absent();
    this.considerHealthyAfterRunningForSeconds = Optional.absent();
    this.serviceBasePath = Optional.absent();
    this.loadBalancerGroups = Optional.absent();
    this.loadBalancerOptions = Optional.absent();
  }

  public SingularityDeploy build() {

    // Prefer using the resource request list over the resources element.
    Optional<Resources> buildResources = this.resources;
    Optional<List<SingularityResourceRequest>> buildRequestedResources = this.requestedResources;

    if (buildResources.isPresent()) {
      buildRequestedResources = Optional.of(resources.get().getAsResourceRequestList());
      buildResources = Optional.absent();
    }

    return new SingularityDeploy(requestId, id, command, arguments, containerInfo, customExecutorCmd, customExecutorId, customExecutorSource, buildResources, buildRequestedResources, env, uris, metadata, executorData, version, timestamp, deployHealthTimeoutSeconds, healthcheckUri,
        healthcheckIntervalSeconds, healthcheckTimeoutSeconds, serviceBasePath, loadBalancerGroups, considerHealthyAfterRunningForSeconds, loadBalancerOptions, skipHealthchecksOnDeploy);
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

  public Optional<List<SingularityResourceRequest>> getRequestedResources() {
    return requestedResources;
  }

  public SingularityDeployBuilder setRequestedResources(Optional<List<SingularityResourceRequest>> requestedResources) {
    this.requestedResources = requestedResources;
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

  public Optional<List<String>> getUris() {
    return uris;
  }

  public SingularityDeployBuilder setUris(Optional<List<String>> uris) {
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

  public Optional<String> getServiceBasePath() {
    return serviceBasePath;
  }

  public SingularityDeployBuilder setServiceBasePath(Optional<String> serviceBasePath) {
    this.serviceBasePath = serviceBasePath;
    return this;
  }

  public Optional<List<String>> getLoadBalancerGroups() {
    return loadBalancerGroups;
  }

  public SingularityDeployBuilder setLoadBalancerGroups(Optional<List<String>> loadBalancerGroups) {
    this.loadBalancerGroups = loadBalancerGroups;
    return this;
  }

  public Optional<Map<String, Object>> getLoadBalancerOptions() {
    return loadBalancerOptions;
  }

  public SingularityDeployBuilder setLoadBalancerOptions(Optional<Map<String, Object>> loadBalancerOptions) {
    this.loadBalancerOptions = loadBalancerOptions;
    return this;
  }

  public Optional<Boolean> getSkipHealthchecksOnDeploy() {
    return skipHealthchecksOnDeploy;
  }

  public SingularityDeployBuilder setSkipHealthchecksOnDeploy(Optional<Boolean> skipHealthchecksOnDeploy) {
    this.skipHealthchecksOnDeploy = skipHealthchecksOnDeploy;
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }
}
