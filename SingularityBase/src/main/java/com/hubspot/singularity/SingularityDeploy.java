package com.hubspot.singularity;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.hubspot.deploy.ExecutorData;
import com.hubspot.mesos.Resources;
import com.hubspot.mesos.SingularityContainerInfo;
import com.hubspot.mesos.SingularityResourceRequest;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class SingularityDeploy extends SingularityJsonObject {

  private final String requestId;

  private final String id;

  private final Optional<String> version;
  private final Optional<Long> timestamp;
  private final Optional<Map<String, String>> metadata;

  private final Optional<SingularityContainerInfo> containerInfo;

  private final Optional<String> customExecutorCmd;
  private final Optional<String> customExecutorId;
  private final Optional<String> customExecutorSource;
  private final Optional<Resources> resources;
  private final Optional<List<SingularityResourceRequest>> requestedResources;

  private final Optional<String> command;
  private final Optional<List<String>> arguments;
  private final Optional<Map<String, String>> env;
  private final Optional<List<String>> uris;
  private final Optional<ExecutorData> executorData;

  private final Optional<String> healthcheckUri;
  private final Optional<Long> healthcheckIntervalSeconds;
  private final Optional<Long> healthcheckTimeoutSeconds;
  private final Optional<Boolean> skipHealthchecksOnDeploy;

  private final Optional<Long> deployHealthTimeoutSeconds;

  private final Optional<Long> considerHealthyAfterRunningForSeconds;

  private final Optional<String> serviceBasePath;
  private final Optional<List<String>> loadBalancerGroups;
  private final Optional<Map<String, Object>> loadBalancerOptions;

  public static SingularityDeployBuilder newBuilder(String requestId, String id) {
    return new SingularityDeployBuilder(requestId, id);
  }

  public static SingularityDeploy fromBytes(byte[] bytes, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(bytes, SingularityDeploy.class);
    } catch (IOException e) {
      throw new SingularityJsonException(e);
    }
  }

  @JsonCreator
  public SingularityDeploy(@JsonProperty("requestId") String requestId,
      @JsonProperty("id") String id,
      @JsonProperty("command") Optional<String> command,
      @JsonProperty("arguments") Optional<List<String>> arguments,
      @JsonProperty("containerInfo") Optional<SingularityContainerInfo> containerInfo,
      @JsonProperty("customExecutorCmd") Optional<String> customExecutorCmd,
      @JsonProperty("customExecutorId") Optional<String> customExecutorId,
      @JsonProperty("customExecutorSource") Optional<String> customExecutorSource,
      @JsonProperty("resources") Optional<Resources> resources,
      @JsonProperty("requestedResources") Optional<List<SingularityResourceRequest>> requestedResources,
      @JsonProperty("env") Optional<Map<String, String>> env,
      @JsonProperty("uris") Optional<List<String>> uris,
      @JsonProperty("metadata") Optional<Map<String, String>> metadata,
      @JsonProperty("executorData") Optional<ExecutorData> executorData,
      @JsonProperty("version") Optional<String> version,
      @JsonProperty("timestamp") Optional<Long> timestamp,
      @JsonProperty("deployHealthTimeoutSeconds") Optional<Long> deployHealthTimeoutSeconds,
      @JsonProperty("healthcheckUri") Optional<String> healthcheckUri,
      @JsonProperty("healthcheckIntervalSeconds") Optional<Long> healthcheckIntervalSeconds,
      @JsonProperty("healthcheckTimeoutSeconds") Optional<Long> healthcheckTimeoutSeconds,
      @JsonProperty("serviceBasePath") Optional<String> serviceBasePath,
      @JsonProperty("loadBalancerGroups") Optional<List<String>> loadBalancerGroups,
      @JsonProperty("considerHealthyAfterRunningForSeconds") Optional<Long> considerHealthyAfterRunningForSeconds,
      @JsonProperty("loadBalancerOptions") Optional<Map<String, Object>> loadBalancerOptions,
      @JsonProperty("skipHealthchecksOnDeploy") Optional<Boolean> skipHealthchecksOnDeploy) {
    this.requestId = requestId;

    this.command = command;
    this.arguments = arguments;
    this.resources = resources;
    this.requestedResources = requestedResources;

    this.containerInfo = containerInfo;

    this.customExecutorCmd = customExecutorCmd;
    this.customExecutorId = customExecutorId;
    this.customExecutorSource = customExecutorSource;

    this.metadata = metadata;
    this.version = version;
    this.id = id;
    this.timestamp = timestamp;
    this.env = env;
    this.uris = uris;
    this.executorData = executorData;

    this.healthcheckUri = healthcheckUri;
    this.healthcheckIntervalSeconds = healthcheckIntervalSeconds;
    this.healthcheckTimeoutSeconds = healthcheckTimeoutSeconds;
    this.skipHealthchecksOnDeploy = skipHealthchecksOnDeploy;

    this.considerHealthyAfterRunningForSeconds = considerHealthyAfterRunningForSeconds;

    this.deployHealthTimeoutSeconds = deployHealthTimeoutSeconds;

    this.serviceBasePath = serviceBasePath;
    this.loadBalancerGroups = loadBalancerGroups;
    this.loadBalancerOptions = loadBalancerOptions;
  }

  public SingularityDeployBuilder toBuilder() {
    return new SingularityDeployBuilder(requestId, id)
    .setCommand(command)
    .setArguments(copyOfList(arguments))
    .setResources(resources)
    .setRequestedResources(requestedResources)
    .setContainerInfo(containerInfo)
    .setCustomExecutorCmd(customExecutorCmd)
    .setCustomExecutorId(customExecutorId)
    .setCustomExecutorSource(customExecutorSource)
    .setHealthcheckUri(healthcheckUri)
    .setHealthcheckIntervalSeconds(healthcheckIntervalSeconds)
    .setHealthcheckTimeoutSeconds(healthcheckTimeoutSeconds)
    .setSkipHealthchecksOnDeploy(skipHealthchecksOnDeploy)
    .setConsiderHealthyAfterRunningForSeconds(considerHealthyAfterRunningForSeconds)
    .setDeployHealthTimeoutSeconds(deployHealthTimeoutSeconds)
    .setServiceBasePath(serviceBasePath)
    .setLoadBalancerGroups(copyOfList(loadBalancerGroups))
    .setLoadBalancerOptions(copyOfMap(loadBalancerOptions))
    .setMetadata(copyOfMap(metadata))
    .setVersion(version)
    .setTimestamp(timestamp)
    .setEnv(copyOfMap(env))
    .setUris(copyOfList(uris))
    .setExecutorData(executorData);
  }

  @ApiModelProperty(required = false, value = "Number of seconds that singularity waits for this service to become healthy.")
  public Optional<Long> getDeployHealthTimeoutSeconds() {
    return deployHealthTimeoutSeconds;
  }

  @ApiModelProperty(required = true, value = "Singularity Request Id which is associated with this deploy.")
  public String getRequestId() {
    return requestId;
  }

  @ApiModelProperty(required = true, value = "Singularity deploy id.")
  public String getId() {
    return id;
  }

  @ApiModelProperty(required = false, value = "Deploy version")
  public Optional<String> getVersion() {
    return version;
  }

  @ApiModelProperty(required = false, value = "Deploy timestamp.")
  public Optional<Long> getTimestamp() {
    return timestamp;
  }

  @ApiModelProperty(required = false, value = "Map of metadata key/value pairs associated with the deployment.")
  public Optional<Map<String, String>> getMetadata() {
    return metadata;
  }

  @ApiModelProperty(required = false, value = "Container information for deployment into a container.", dataType = "SingularityContainerInfo")
  public Optional<SingularityContainerInfo> getContainerInfo() {
    return containerInfo;
  }

  @ApiModelProperty(required = false, value = "Custom Mesos executor", dataType = "string")
  public Optional<String> getCustomExecutorCmd() {
    return customExecutorCmd;
  }

  @ApiModelProperty(required = false, value = "Custom Mesos executor id.")
  public Optional<String> getCustomExecutorId() {
    return customExecutorId;
  }

  @ApiModelProperty(required = false, value = "Custom Mesos executor source.")
  public Optional<String> getCustomExecutorSource() {
    return customExecutorSource;
  }

  @ApiModelProperty(required = false, value = "Resources required for this deploy.", dataType = "com.hubspot.mesos.Resources")
  public Optional<Resources> getResources() {
    return resources;
  }

  @ApiModelProperty(required = false, value = "Resources required for this deploy.")
  public Optional<List<SingularityResourceRequest>> getRequestedResources() {
    return requestedResources;
  }

  @ApiModelProperty(required = false, value = "Command to execute for this deployment.")
  public Optional<String> getCommand() {
    return command;
  }

  @ApiModelProperty(required = false, value = "Command arguments.")
  public Optional<List<String>> getArguments() {
    return arguments;
  }

  @ApiModelProperty(required = false, value = "Map of environment variable definitions.")
  public Optional<Map<String, String>> getEnv() {
    return env;
  }

  @ApiModelProperty(required = false, value = "List of URIs to download before executing the deploy command.")
  public Optional<List<String>> getUris() {
    return uris;
  }

  @ApiModelProperty(required = false, value = "Executor specific information")
  public Optional<ExecutorData> getExecutorData() {
    return executorData;
  }

  @ApiModelProperty(required = false, value = "Deployment Healthcheck URI.")
  public Optional<String> getHealthcheckUri() {
    return healthcheckUri;
  }

  @ApiModelProperty(required = false, value = "Health check interval in seconds.")
  public Optional<Long> getHealthcheckIntervalSeconds() {
    return healthcheckIntervalSeconds;
  }

  @ApiModelProperty(required = false, value = "Health check timeout in seconds.")
  public Optional<Long> getHealthcheckTimeoutSeconds() {
    return healthcheckTimeoutSeconds;
  }

  @ApiModelProperty(required = false, value = "The base path for the API exposed by the deploy. Used in conjunction with the Load balancer API.")
  public Optional<String> getServiceBasePath() {
    return serviceBasePath;
  }

  @ApiModelProperty(required = false, value = "Number of seconds that a service must be healthy to consider the deployment to be successful.")
  public Optional<Long> getConsiderHealthyAfterRunningForSeconds() {
    return considerHealthyAfterRunningForSeconds;
  }

  @ApiModelProperty(required = false, value = "List of load balancer groups associated with this deployment.")
  public Optional<List<String>> getLoadBalancerGroups() {
    return loadBalancerGroups;
  }

  @ApiModelProperty(required = false, value = "Map (Key/Value) of options for the load balancer.")
  public Optional<Map<String, Object>> getLoadBalancerOptions() {
    return loadBalancerOptions;
  }

  @ApiModelProperty(required = false, value = "Allows skipping of health checks when deploying.")
  public Optional<Boolean> getSkipHealthchecksOnDeploy() {
    return skipHealthchecksOnDeploy;
  }

  /**
   * Returns the lists of resources requested by this deploy. This is either the
   * resources defined in the {@link Resources} object or the list of
   * {@link SingularityResourceRequest} elements. If a {@link Resources} object
   * is present, it takes precedence.
   */
  @JsonIgnore
  public Optional<List<SingularityResourceRequest>> getResourceRequestList() {
    if (resources.isPresent()) {
      return Optional.of(resources.get().getAsResourceRequestList());
    } else {
      return getRequestedResources();
    }
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }
}
