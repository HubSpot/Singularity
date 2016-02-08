package com.hubspot.singularity;

import static com.hubspot.singularity.JsonHelpers.copyOfList;
import static com.hubspot.singularity.JsonHelpers.copyOfSet;
import static com.hubspot.singularity.JsonHelpers.copyOfMap;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.deploy.ExecutorData;
import com.hubspot.mesos.Resources;
import com.hubspot.mesos.SingularityContainerInfo;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class SingularityDeploy {

  private final String requestId;

  private final String id;

  private final Optional<String> version;
  private final Optional<Long> timestamp;
  private final Optional<Map<String, String>> metadata;

  private final Optional<SingularityContainerInfo> containerInfo;

  private final Optional<String> customExecutorCmd;
  private final Optional<String> customExecutorId;
  private final Optional<String> customExecutorSource;
  private final Optional<Resources> customExecutorResources;
  private final Optional<String> customExecutorUser;

  private final Optional<Resources> resources;

  private final Optional<String> command;
  private final Optional<List<String>> arguments;
  private final Optional<Map<String, String>> env;
  private final Optional<List<String>> uris;
  private final Optional<ExecutorData> executorData;
  private final Optional<Map<String, String>> labels;

  private final Optional<String> healthcheckUri;
  private final Optional<Long> healthcheckIntervalSeconds;
  private final Optional<Long> healthcheckTimeoutSeconds;
  private final Optional<Boolean> skipHealthchecksOnDeploy;
  private final Optional<HealthcheckProtocol> healthcheckProtocol;

  private final Optional<Integer> healthcheckMaxRetries;
  private final Optional<Long> healthcheckMaxTotalTimeoutSeconds;

  private final Optional<Long> deployHealthTimeoutSeconds;

  private final Optional<Long> considerHealthyAfterRunningForSeconds;

  private final Optional<String> serviceBasePath;
  private final Optional<Set<String>> loadBalancerGroups;
  private final Optional<Map<String, Object>> loadBalancerOptions;

  private final Optional<Integer> deployInstanceCountPerStep;
  private final Optional<Integer> deployStepWaitTimeSeconds;
  private final Optional<Boolean> autoAdvanceDeploySteps;
  private final Optional<Integer> maxTaskRetries;

  public static SingularityDeployBuilder newBuilder(String requestId, String id) {
    return new SingularityDeployBuilder(requestId, id);
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
      @JsonProperty("customExecutorResources") Optional<Resources> customExecutorResources,
      @JsonProperty("customExecutorUser") Optional<String> customExecutorUser,
      @JsonProperty("resources") Optional<Resources> resources,
      @JsonProperty("env") Optional<Map<String, String>> env,
      @JsonProperty("uris") Optional<List<String>> uris,
      @JsonProperty("metadata") Optional<Map<String, String>> metadata,
      @JsonProperty("executorData") Optional<ExecutorData> executorData,
      @JsonProperty("version") Optional<String> version,
      @JsonProperty("timestamp") Optional<Long> timestamp,
      @JsonProperty("labels") Optional<Map<String, String>> labels,
      @JsonProperty("deployHealthTimeoutSeconds") Optional<Long> deployHealthTimeoutSeconds,
      @JsonProperty("healthcheckUri") Optional<String> healthcheckUri,
      @JsonProperty("healthcheckIntervalSeconds") Optional<Long> healthcheckIntervalSeconds,
      @JsonProperty("healthcheckTimeoutSeconds") Optional<Long> healthcheckTimeoutSeconds,
      @JsonProperty("healthcheckMaxRetries") Optional<Integer> healthcheckMaxRetries,
      @JsonProperty("healthcheckMaxTotalTimeoutSeconds") Optional<Long> healthcheckMaxTotalTimeoutSeconds,
      @JsonProperty("serviceBasePath") Optional<String> serviceBasePath,
      @JsonProperty("loadBalancerGroups") Optional<Set<String>> loadBalancerGroups,
      @JsonProperty("considerHealthyAfterRunningForSeconds") Optional<Long> considerHealthyAfterRunningForSeconds,
      @JsonProperty("loadBalancerOptions") Optional<Map<String, Object>> loadBalancerOptions,
      @JsonProperty("skipHealthchecksOnDeploy") Optional<Boolean> skipHealthchecksOnDeploy,
      @JsonProperty("healthCheckProtocol") Optional<HealthcheckProtocol> healthcheckProtocol,
      @JsonProperty("deployInstanceCountPerStep") Optional<Integer> deployInstanceCountPerStep,
      @JsonProperty("deployStepWaitTimeMs") Optional<Integer> deployStepWaitTimeSeconds,
      @JsonProperty("autoAdvanceDeploySteps") Optional<Boolean> autoAdvanceDeploySteps,
      @JsonProperty("maxTaskRetries") Optional<Integer> maxTaskRetries) {
    this.requestId = requestId;

    this.command = command;
    this.arguments = arguments;
    this.resources = resources;

    this.containerInfo = containerInfo;

    this.customExecutorCmd = customExecutorCmd;
    this.customExecutorId = customExecutorId;
    this.customExecutorSource = customExecutorSource;
    this.customExecutorResources = customExecutorResources;
    this.customExecutorUser = customExecutorUser;

    this.metadata = metadata;
    this.version = version;
    this.id = id;
    this.timestamp = timestamp;
    this.env = env;
    this.uris = uris;
    this.executorData = executorData;
    this.labels = labels;

    this.healthcheckUri = healthcheckUri;
    this.healthcheckIntervalSeconds = healthcheckIntervalSeconds;
    this.healthcheckTimeoutSeconds = healthcheckTimeoutSeconds;
    this.skipHealthchecksOnDeploy = skipHealthchecksOnDeploy;
    this.healthcheckProtocol = healthcheckProtocol;

    this.healthcheckMaxRetries = healthcheckMaxRetries;
    this.healthcheckMaxTotalTimeoutSeconds = healthcheckMaxTotalTimeoutSeconds;

    this.considerHealthyAfterRunningForSeconds = considerHealthyAfterRunningForSeconds;

    this.deployHealthTimeoutSeconds = deployHealthTimeoutSeconds;

    this.serviceBasePath = serviceBasePath;
    this.loadBalancerGroups = loadBalancerGroups;
    this.loadBalancerOptions = loadBalancerOptions;

    this.deployInstanceCountPerStep = deployInstanceCountPerStep;
    this.deployStepWaitTimeSeconds = deployStepWaitTimeSeconds;
    this.autoAdvanceDeploySteps = autoAdvanceDeploySteps;
    this.maxTaskRetries = maxTaskRetries;
  }

  public SingularityDeployBuilder toBuilder() {
    return new SingularityDeployBuilder(requestId, id)
    .setCommand(command)
    .setArguments(copyOfList(arguments))
    .setResources(resources)
    .setContainerInfo(containerInfo)
    .setCustomExecutorCmd(customExecutorCmd)
    .setCustomExecutorId(customExecutorId)
    .setCustomExecutorSource(customExecutorSource)
    .setCustomExecutorResources(customExecutorResources)
    .setCustomExecutorUser(customExecutorUser)
    .setHealthcheckUri(healthcheckUri)
    .setHealthcheckIntervalSeconds(healthcheckIntervalSeconds)
    .setHealthcheckTimeoutSeconds(healthcheckTimeoutSeconds)
    .setSkipHealthchecksOnDeploy(skipHealthchecksOnDeploy)
    .setHealthcheckProtocol(healthcheckProtocol)
    .setHealthcheckMaxRetries(healthcheckMaxRetries)
    .setHealthcheckMaxTotalTimeoutSeconds(healthcheckMaxTotalTimeoutSeconds)
    .setConsiderHealthyAfterRunningForSeconds(considerHealthyAfterRunningForSeconds)
    .setDeployHealthTimeoutSeconds(deployHealthTimeoutSeconds)
    .setServiceBasePath(serviceBasePath)
    .setLoadBalancerGroups(copyOfSet(loadBalancerGroups))
    .setLoadBalancerOptions(copyOfMap(loadBalancerOptions))
    .setMetadata(copyOfMap(metadata))
    .setVersion(version)
    .setTimestamp(timestamp)
    .setEnv(copyOfMap(env))
    .setUris(copyOfList(uris))
    .setExecutorData(executorData)
    .setLabels(labels)
    .setDeployInstanceCountPerStep(deployInstanceCountPerStep)
    .setDeployStepWaitTimeSeconds(deployStepWaitTimeSeconds)
    .setAutoAdvanceDeploySteps(autoAdvanceDeploySteps)
    .setMaxTaskRetries(maxTaskRetries);
  }

  @ApiModelProperty(required=false, value="Number of seconds that Singularity waits for this service to become healthy (for it to download artifacts, start running, and optionally pass healthchecks.)")
  public Optional<Long> getDeployHealthTimeoutSeconds() {
    return deployHealthTimeoutSeconds;
  }

  @ApiModelProperty(required=true, value="Singularity Request Id which is associated with this deploy.")
  public String getRequestId() {
    return requestId;
  }

  @ApiModelProperty(required=true, value="Singularity deploy id.")
  public String getId() {
    return id;
  }

  @ApiModelProperty(required=false, value="Deploy version")
  public Optional<String> getVersion() {
    return version;
  }

  @ApiModelProperty(required=false, value="Deploy timestamp.")
  public Optional<Long> getTimestamp() {
    return timestamp;
  }

  @ApiModelProperty(required=false, value="Map of metadata key/value pairs associated with the deployment.")
  public Optional<Map<String, String>> getMetadata() {
    return metadata;
  }

  @ApiModelProperty(required=false, value="Container information for deployment into a container.", dataType="SingularityContainerInfo")
  public Optional<SingularityContainerInfo> getContainerInfo() {
    return containerInfo;
  }

  @ApiModelProperty(required=false, value="Custom Mesos executor", dataType= "string")
  public Optional<String> getCustomExecutorCmd() {
    return customExecutorCmd;
  }

  @ApiModelProperty(required=false, value="Custom Mesos executor id.")
  public Optional<String> getCustomExecutorId() {
    return customExecutorId;
  }

  @ApiModelProperty(required=false, value="Custom Mesos executor source.")
  public Optional<String> getCustomExecutorSource() { return customExecutorSource; }

  @ApiModelProperty(required=false, value="Resources to allocate for custom mesos executor")
  public Optional<Resources> getCustomExecutorResources() {
    return customExecutorResources;
  }

  @Deprecated
  @ApiModelProperty(required=false, value="User to run custom executor as")
  public Optional<String> getCustomExecutorUser() {
    return customExecutorUser;
  }

  @ApiModelProperty(required=false, value="Resources required for this deploy.", dataType="com.hubspot.mesos.Resources")
  public Optional<Resources> getResources() {
    return resources;
  }

  @ApiModelProperty(required=false, value="Command to execute for this deployment.")
  public Optional<String> getCommand() {
    return command;
  }

  @ApiModelProperty(required=false, value="Command arguments.")
  public Optional<List<String>> getArguments() {
    return arguments;
  }

  @ApiModelProperty(required=false, value="Map of environment variable definitions.")
  public Optional<Map<String, String>> getEnv() {
    return env;
  }

  @ApiModelProperty(required=false, value="List of URIs to download before executing the deploy command.")
  public Optional<List<String>> getUris() {
    return uris;
  }

  @ApiModelProperty(required=false, value="Executor specific information")
  public Optional<ExecutorData> getExecutorData() {
    return executorData;
  }

  @ApiModelProperty(required=false, value="Deployment Healthcheck URI, if specified will be called after TASK_RUNNING.")
  public Optional<String> getHealthcheckUri() {
    return healthcheckUri;
  }

  @ApiModelProperty(required=false, value="Healthcheck protocol - HTTP or HTTPS")
  public Optional<HealthcheckProtocol> getHealthcheckProtocol() {
    return healthcheckProtocol;
  }

  @ApiModelProperty(required=false, value="Time to wait after a failed healthcheck to try again in seconds.")
  public Optional<Long> getHealthcheckIntervalSeconds() {
    return healthcheckIntervalSeconds;
  }

  @ApiModelProperty(required=false, value="Single healthcheck HTTP timeout in seconds.")
  public Optional<Long> getHealthcheckTimeoutSeconds() {
    return healthcheckTimeoutSeconds;
  }

  @ApiModelProperty(required=false, value="The base path for the API exposed by the deploy. Used in conjunction with the Load balancer API.")
  public Optional<String> getServiceBasePath() {
    return serviceBasePath;
  }

  @ApiModelProperty(required=false, value="Number of seconds that a service must be healthy to consider the deployment to be successful.")
  public Optional<Long> getConsiderHealthyAfterRunningForSeconds() {
    return considerHealthyAfterRunningForSeconds;
  }

  @ApiModelProperty(required=false, value="List of load balancer groups associated with this deployment.")
  public Optional<Set<String>> getLoadBalancerGroups() {
    return loadBalancerGroups;
  }

  @ApiModelProperty(required=false, value="Map (Key/Value) of options for the load balancer.")
  public Optional<Map<String, Object>> getLoadBalancerOptions() {
    return loadBalancerOptions;
  }

  @ApiModelProperty(required=false, value="Labels for tasks associated with this deploy")
  public Optional<Map<String, String>> getLabels() {
    return labels;
  }

  @ApiModelProperty(required=false, value="Allows skipping of health checks when deploying.")
  public Optional<Boolean> getSkipHealthchecksOnDeploy() {
    return skipHealthchecksOnDeploy;
  }

  @ApiModelProperty(required=false, value="Maximum number of times to retry an individual healthcheck before failing the deploy.")
  public Optional<Integer> getHealthcheckMaxRetries() {
    return healthcheckMaxRetries;
  }

  @ApiModelProperty(required=false, value="Maximum amount of time to wait before failing a deploy for healthchecks to pass.")
  public Optional<Long> getHealthcheckMaxTotalTimeoutSeconds() {
    return healthcheckMaxTotalTimeoutSeconds;
  }

  @ApiModelProperty(required=false, value="deploy this many instances at a time")
  public Optional<Integer> getDeployInstanceCountPerStep() {
    return deployInstanceCountPerStep;
  }

  @ApiModelProperty(required=false, value="wait this long between deploy steps")
  public Optional<Integer> getDeployStepWaitTimeSeconds() {
    return deployStepWaitTimeSeconds;
  }

  @ApiModelProperty(required=false, value="automatically advance to the next target instance count after `deployStepWaitTimeSeconds` seconds")
  public Optional<Boolean> getAutoAdvanceDeploySteps() {
    return autoAdvanceDeploySteps;
  }

  @ApiModelProperty(required=false, value="allowed at most this many failed tasks to be retried before failing the deploy")
  public Optional<Integer> getMaxTaskRetries() {
    return maxTaskRetries;
  }

  @Override
  public String toString() {
    return "SingularityDeploy{" +
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
      ", customExecutorUser=" + customExecutorUser +
      ", resources=" + resources +
      ", command=" + command +
      ", arguments=" + arguments +
      ", env=" + env +
      ", uris=" + uris +
      ", executorData=" + executorData +
      ", healthcheckUri=" + healthcheckUri +
      ", healthcheckIntervalSeconds=" + healthcheckIntervalSeconds +
      ", healthcheckTimeoutSeconds=" + healthcheckTimeoutSeconds +
      ", skipHealthchecksOnDeploy=" + skipHealthchecksOnDeploy +
      ", healthcheckProtocol=" + healthcheckProtocol +
      ", healthcheckMaxRetries=" + healthcheckMaxRetries +
      ", healthcheckMaxTotalTimeoutSeconds=" + healthcheckMaxTotalTimeoutSeconds +
      ", deployHealthTimeoutSeconds=" + deployHealthTimeoutSeconds +
      ", considerHealthyAfterRunningForSeconds=" + considerHealthyAfterRunningForSeconds +
      ", serviceBasePath=" + serviceBasePath +
      ", loadBalancerGroups=" + loadBalancerGroups +
      ", loadBalancerOptions=" + loadBalancerOptions +
      ", labels=" + labels +
      ", deployInstanceCountPerStep=" + deployInstanceCountPerStep +
      ", deployStepWaitTimeSeconds=" + deployStepWaitTimeSeconds +
      ", autoAdvanceDeploySteps=" + autoAdvanceDeploySteps +
      ", maxTaskRetries=" + maxTaskRetries +
      '}';
  }

}
