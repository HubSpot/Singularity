package com.hubspot.singularity;

import static com.hubspot.singularity.JsonHelpers.copyOfList;
import static com.hubspot.singularity.JsonHelpers.copyOfMap;
import static com.hubspot.singularity.JsonHelpers.copyOfSet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.deploy.ExecutorData;
import com.hubspot.deploy.HealthcheckOptions;
import com.hubspot.mesos.Resources;
import com.hubspot.mesos.SingularityContainerInfo;
import com.hubspot.mesos.SingularityMesosArtifact;
import com.hubspot.mesos.SingularityMesosTaskLabel;
import com.hubspot.singularity.api.SingularityRunNowRequest;
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

  private final Optional<Resources> resources;

  private final Optional<String> command;
  private final Optional<List<String>> arguments;
  private final Optional<Map<String, String>> env;
  private final Optional<List<SingularityMesosArtifact>> uris;
  private final Optional<ExecutorData> executorData;
  private final Optional<Map<String, String>> labels;
  private final Optional<List<SingularityMesosTaskLabel>> mesosLabels;
  private final Optional<Map<Integer, Map<String, String>>> taskLabels;
  private final Optional<Map<Integer, List<SingularityMesosTaskLabel>>> mesosTaskLabels;
  private final Optional<Map<Integer, Map<String, String>>> taskEnv;

  private final Optional<SingularityRunNowRequest> runImmediatelyRequest;

  /**
   * @deprecated use {@link #healthcheck}
   */
  @Deprecated
  private final Optional<String> healthcheckUri;
  /**
   * @deprecated use {@link #healthcheck}
   */
  @Deprecated
  private final Optional<Long> healthcheckIntervalSeconds;
  /**
   * @deprecated use {@link #healthcheck}
   */
  @Deprecated
  private final Optional<Long> healthcheckTimeoutSeconds;
  /**
   * @deprecated use {@link #healthcheck}
   */
  @Deprecated
  private final Optional<Integer> healthcheckPortIndex;
  /**
   * @deprecated use {@link #healthcheck}
   */
  @Deprecated
  private final Optional<HealthcheckProtocol> healthcheckProtocol;
  /**
   * @deprecated use {@link #healthcheck}
   */
  @Deprecated
  private final Optional<Integer> healthcheckMaxRetries;
  /**
   * @deprecated use {@link #healthcheck}
   */
  @Deprecated
  private final Optional<Long> healthcheckMaxTotalTimeoutSeconds;

  private final Optional<HealthcheckOptions> healthcheck;

  private final Optional<Boolean> skipHealthchecksOnDeploy;

  private final Optional<Long> deployHealthTimeoutSeconds;

  private final Optional<Long> considerHealthyAfterRunningForSeconds;

  private final Optional<String> serviceBasePath;
  private final Optional<Set<String>> loadBalancerGroups;
  private final Optional<Integer> loadBalancerPortIndex;
  private final Optional<Map<String, Object>> loadBalancerOptions;
  private final Optional<Set<String>> loadBalancerDomains;
  private final Optional<List<String>> loadBalancerAdditionalRoutes;
  private final Optional<String> loadBalancerTemplate;
  private final Optional<String> loadBalancerServiceIdOverride;
  private final Optional<String> loadBalancerUpstreamGroup;

  private final Optional<Integer> deployInstanceCountPerStep;
  private final Optional<Integer> deployStepWaitTimeMs;
  private final Optional<Boolean> autoAdvanceDeploySteps;
  private final Optional<Integer> maxTaskRetries;
  private final Optional<Boolean> shell;
  private final Optional<String> user;

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
      @JsonProperty("resources") Optional<Resources> resources,
      @JsonProperty("env") Optional<Map<String, String>> env,
      @JsonProperty("taskEnv") Optional<Map<Integer, Map<String, String>>> taskEnv,
      @JsonProperty("runImmediately") Optional<SingularityRunNowRequest> runImmediatelyRequest,
      @JsonProperty("uris") Optional<List<SingularityMesosArtifact>> uris,
      @JsonProperty("metadata") Optional<Map<String, String>> metadata,
      @JsonProperty("executorData") Optional<ExecutorData> executorData,
      @JsonProperty("version") Optional<String> version,
      @JsonProperty("timestamp") Optional<Long> timestamp,
      @JsonProperty("labels") Optional<Map<String, String>> labels,
      @JsonProperty("mesosLabels") Optional<List<SingularityMesosTaskLabel>> mesosLabels,
      @JsonProperty("taskLabels") Optional<Map<Integer, Map<String, String>>> taskLabels,
      @JsonProperty("mesosTaskLabels") Optional<Map<Integer, List<SingularityMesosTaskLabel>>> mesosTaskLabels,
      @JsonProperty("deployHealthTimeoutSeconds") Optional<Long> deployHealthTimeoutSeconds,
      @JsonProperty("healthcheckUri") Optional<String> healthcheckUri,
      @JsonProperty("healthcheckIntervalSeconds") Optional<Long> healthcheckIntervalSeconds,
      @JsonProperty("healthcheckTimeoutSeconds") Optional<Long> healthcheckTimeoutSeconds,
      @JsonProperty("healthcheckPortIndex") Optional<Integer> healthcheckPortIndex,
      @JsonProperty("healthcheckMaxRetries") Optional<Integer> healthcheckMaxRetries,
      @JsonProperty("healthcheckMaxTotalTimeoutSeconds") Optional<Long> healthcheckMaxTotalTimeoutSeconds,
      @JsonProperty("healthcheck") Optional<HealthcheckOptions> healthcheck,
      @JsonProperty("serviceBasePath") Optional<String> serviceBasePath,
      @JsonProperty("loadBalancerGroups") Optional<Set<String>> loadBalancerGroups,
      @JsonProperty("loadBalancerPortIndex") Optional<Integer> loadBalancerPortIndex,
      @JsonProperty("considerHealthyAfterRunningForSeconds") Optional<Long> considerHealthyAfterRunningForSeconds,
      @JsonProperty("loadBalancerOptions") Optional<Map<String, Object>> loadBalancerOptions,
      @JsonProperty("loadBalancerDomains") Optional<Set<String>> loadBalancerDomains,
      @JsonProperty("loadBalancerAdditionalRoutes") Optional<List<String>> loadBalancerAdditionalRoutes,
      @JsonProperty("loadBalancerTemplate") Optional<String> loadBalancerTemplate,
      @JsonProperty("loadBalancerServiceIdOverride") Optional<String> loadBalancerServiceIdOverride,
      @JsonProperty("loadBalancerUpstreamGroup") Optional<String> loadBalancerUpstreamGroup,
      @JsonProperty("skipHealthchecksOnDeploy") Optional<Boolean> skipHealthchecksOnDeploy,
      @JsonProperty("healthCheckProtocol") Optional<HealthcheckProtocol> healthcheckProtocol,
      @JsonProperty("deployInstanceCountPerStep") Optional<Integer> deployInstanceCountPerStep,
      @JsonProperty("deployStepWaitTimeMs") Optional<Integer> deployStepWaitTimeMs,
      @JsonProperty("autoAdvanceDeploySteps") Optional<Boolean> autoAdvanceDeploySteps,
      @JsonProperty("maxTaskRetries") Optional<Integer> maxTaskRetries,
      @JsonProperty("shell") Optional<Boolean> shell,
      @JsonProperty("user") Optional<String> user) {
    this.requestId = requestId;

    this.command = command;
    this.arguments = arguments;
    this.resources = resources;

    this.containerInfo = containerInfo;

    this.customExecutorCmd = customExecutorCmd;
    this.customExecutorId = customExecutorId;
    this.customExecutorSource = customExecutorSource;
    this.customExecutorResources = customExecutorResources;

    this.metadata = metadata;
    this.version = version;
    this.id = id;
    this.timestamp = timestamp;
    this.env = env;
    this.taskEnv = taskEnv;
    this.uris = uris;
    this.executorData = executorData;

    this.runImmediatelyRequest = runImmediatelyRequest;

    this.labels = labels;
    this.mesosLabels = mesosLabels.or(labels.isPresent() ? Optional.of(SingularityMesosTaskLabel.labelsFromMap(labels.get())) : Optional.<List<SingularityMesosTaskLabel>>absent());
    this.taskLabels = taskLabels;
    this.mesosTaskLabels = mesosTaskLabels.or(taskLabels.isPresent() ? Optional.of(parseMesosTaskLabelsFromMap(taskLabels.get())) : Optional.<Map<Integer,List<SingularityMesosTaskLabel>>>absent());

    this.healthcheckUri = healthcheckUri;
    this.healthcheckIntervalSeconds = healthcheckIntervalSeconds;
    this.healthcheckTimeoutSeconds = healthcheckTimeoutSeconds;
    this.healthcheckPortIndex = healthcheckPortIndex;
    this.skipHealthchecksOnDeploy = skipHealthchecksOnDeploy;
    this.healthcheckProtocol = healthcheckProtocol;

    this.healthcheckMaxRetries = healthcheckMaxRetries;
    this.healthcheckMaxTotalTimeoutSeconds = healthcheckMaxTotalTimeoutSeconds;

    if (healthcheckUri.isPresent() && !healthcheck.isPresent()) {
      this.healthcheck = Optional.of(new HealthcheckOptions(
        healthcheckUri.get(),
        healthcheckPortIndex,
        Optional.<Long>absent(),
        healthcheckProtocol,
        Optional.<Integer>absent(),
        Optional.<Integer>absent(),
        Optional.<Integer>absent(),
        healthcheckIntervalSeconds.isPresent() ? Optional.of(healthcheckIntervalSeconds.get().intValue()) : Optional.<Integer>absent(),
        healthcheckTimeoutSeconds.isPresent() ? Optional.of(healthcheckTimeoutSeconds.get().intValue()) : Optional.<Integer>absent(),
        healthcheckMaxRetries,
        Optional.<List<Integer>>absent()));
    } else {
      this.healthcheck = healthcheck;
    }
    this.considerHealthyAfterRunningForSeconds = considerHealthyAfterRunningForSeconds;

    this.deployHealthTimeoutSeconds = deployHealthTimeoutSeconds;

    this.serviceBasePath = serviceBasePath;
    this.loadBalancerGroups = loadBalancerGroups;
    this.loadBalancerPortIndex = loadBalancerPortIndex;
    this.loadBalancerOptions = loadBalancerOptions;
    this.loadBalancerDomains = loadBalancerDomains;
    this.loadBalancerAdditionalRoutes = loadBalancerAdditionalRoutes;
    this.loadBalancerTemplate = loadBalancerTemplate;
    this.loadBalancerServiceIdOverride = loadBalancerServiceIdOverride;
    this.loadBalancerUpstreamGroup = loadBalancerUpstreamGroup;

    this.deployInstanceCountPerStep = deployInstanceCountPerStep;
    this.deployStepWaitTimeMs = deployStepWaitTimeMs;
    this.autoAdvanceDeploySteps = autoAdvanceDeploySteps;
    this.maxTaskRetries = maxTaskRetries;
    this.shell = shell;
    this.user = user;
  }

  private static Map<Integer, List<SingularityMesosTaskLabel>> parseMesosTaskLabelsFromMap(Map<Integer, Map<String, String>> taskLabels) {
    Map<Integer, List<SingularityMesosTaskLabel>> mesosTaskLabels = new HashMap<>();
    for (Map.Entry<Integer, Map<String, String>> entry : taskLabels.entrySet()) {
      mesosTaskLabels.put(entry.getKey(), SingularityMesosTaskLabel.labelsFromMap(entry.getValue()));
    }
    return mesosTaskLabels;
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
    .setHealthcheckUri(healthcheckUri)
    .setHealthcheckIntervalSeconds(healthcheckIntervalSeconds)
    .setHealthcheckTimeoutSeconds(healthcheckTimeoutSeconds)
    .setHealthcheckPortIndex(healthcheckPortIndex)
    .setSkipHealthchecksOnDeploy(skipHealthchecksOnDeploy)
    .setHealthcheckProtocol(healthcheckProtocol)
    .setHealthcheckMaxRetries(healthcheckMaxRetries)
    .setHealthcheckMaxTotalTimeoutSeconds(healthcheckMaxTotalTimeoutSeconds)
    .setHealthcheck(healthcheck)
    .setConsiderHealthyAfterRunningForSeconds(considerHealthyAfterRunningForSeconds)
    .setDeployHealthTimeoutSeconds(deployHealthTimeoutSeconds)
    .setServiceBasePath(serviceBasePath)
    .setLoadBalancerGroups(copyOfSet(loadBalancerGroups))
    .setLoadBalancerPortIndex(loadBalancerPortIndex)
    .setLoadBalancerOptions(copyOfMap(loadBalancerOptions))
    .setLoadBalancerDomains(copyOfSet(loadBalancerDomains))
    .setLoadBalancerAdditionalRoutes(copyOfList(loadBalancerAdditionalRoutes))
    .setLoadBalancerTemplate(loadBalancerTemplate)
    .setLoadBalancerUpstreamGroup(loadBalancerUpstreamGroup)
    .setLoadBalancerServiceIdOverride(loadBalancerServiceIdOverride)
    .setMetadata(copyOfMap(metadata))
    .setVersion(version)
    .setTimestamp(timestamp)
    .setEnv(copyOfMap(env))
    .setTaskEnv(taskEnv)
    .setUris(uris)
    .setExecutorData(executorData)
    .setLabels(labels)
    .setMesosLabels(mesosLabels)
    .setTaskLabels(taskLabels)
    .setMesosTaskLabels(mesosTaskLabels)
    .setDeployInstanceCountPerStep(deployInstanceCountPerStep)
    .setDeployStepWaitTimeMs(deployStepWaitTimeMs)
    .setAutoAdvanceDeploySteps(autoAdvanceDeploySteps)
    .setMaxTaskRetries(maxTaskRetries)
    .setShell(shell)
    .setUser(user);
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

  @ApiModelProperty(required=false, value="Custom Mesos executor", dataType="string")
  public Optional<String> getCustomExecutorCmd() {
    return customExecutorCmd;
  }

  @ApiModelProperty(required=false, value="Custom Mesos executor id.", dataType="string")
  public Optional<String> getCustomExecutorId() {
    return customExecutorId;
  }

  @ApiModelProperty(required=false, value="Custom Mesos executor source.", dataType="string")
  public Optional<String> getCustomExecutorSource() { return customExecutorSource; }

  @ApiModelProperty(required=false, value="Resources to allocate for custom mesos executor", dataType="com.hubspot.mesos.Resources")
  public Optional<Resources> getCustomExecutorResources() {
    return customExecutorResources;
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

  @ApiModelProperty(required=false, value="Map of environment variable overrides for specific task instances.")
  public Optional<Map<Integer, Map<String, String>>> getTaskEnv() {
    return taskEnv;
  }

  @ApiModelProperty(required = false, value = "Settings used to run this deploy immediately")
  public Optional<SingularityRunNowRequest> getRunImmediately() {
    return runImmediatelyRequest;
  }

  @ApiModelProperty(required=false, value="List of URIs to download before executing the deploy command.")
  public Optional<List<SingularityMesosArtifact>> getUris() {
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

  @ApiModelProperty(required=false, value="Healthcheck protocol - HTTP or HTTPS", dataType="com.hubspot.singularity.HealthcheckProtocol")
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

  @ApiModelProperty(required=false, value="Perform healthcheck on this dynamically allocated port (e.g. 0 for first port), defaults to first port")
  public Optional<Integer> getHealthcheckPortIndex() {
    return healthcheckPortIndex;
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

  @ApiModelProperty(required=false, value="Send this port to the load balancer api (e.g. 0 for first port), defaults to first port")
  public Optional<Integer> getLoadBalancerPortIndex() {
    return loadBalancerPortIndex;
  }

  @ApiModelProperty(required=false, value="Map (Key/Value) of options for the load balancer.")
  public Optional<Map<String, Object>> getLoadBalancerOptions() {
    return loadBalancerOptions;
  }

  @ApiModelProperty(required=false, value="List of domains to host this service on, for use with the load balancer api")
  public Optional<Set<String>> getLoadBalancerDomains() {
    return loadBalancerDomains;
  }

  @ApiModelProperty(required=false, value="Additional routes besides serviceBasePath used by this service")
  public Optional<List<String>> getLoadBalancerAdditionalRoutes() {
    return loadBalancerAdditionalRoutes;
  }

  @ApiModelProperty(required=false, value="Name of load balancer template to use if not using the default template")
  public Optional<String> getLoadBalancerTemplate() {
    return loadBalancerTemplate;
  }

  @ApiModelProperty(required=false, value="Name of load balancer Service ID to use instead of the Request ID")
  public Optional<String> getLoadBalancerServiceIdOverride() {
    return loadBalancerServiceIdOverride;
  }

  @ApiModelProperty(required=false, value="Group name to tag all upstreams with in load balancer")
  public Optional<String> getLoadBalancerUpstreamGroup() {
    return loadBalancerUpstreamGroup;
  }

  @Deprecated
  @ApiModelProperty(required=false, value="Labels for all tasks associated with this deploy")
  public Optional<Map<String, String>> getLabels() {
    return labels;
  }

  @ApiModelProperty(required=false, value="Labels for all tasks associated with this deploy")
  public Optional<List<SingularityMesosTaskLabel>> getMesosLabels() {
    return mesosLabels;
  }

  @ApiModelProperty(required=false, value="(Deprecated) Labels for specific tasks associated with this deploy, indexed by instance number")
  public Optional<Map<Integer, Map<String, String>>> getTaskLabels() {
    return taskLabels;
  }

  @ApiModelProperty(required=false, value="Labels for specific tasks associated with this deploy, indexed by instance number")
  public Optional<Map<Integer, List<SingularityMesosTaskLabel>>> getMesosTaskLabels() {
    return mesosTaskLabels;
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

  @ApiModelProperty(required = false, value="HTTP Healthcheck settings")
  public Optional<HealthcheckOptions> getHealthcheck() {
    return healthcheck;
  }

  @ApiModelProperty(required=false, value="deploy this many instances at a time")
  public Optional<Integer> getDeployInstanceCountPerStep() {
    return deployInstanceCountPerStep;
  }

  @ApiModelProperty(required=false, value="wait this long between deploy steps")
  public Optional<Integer> getDeployStepWaitTimeMs() {
    return deployStepWaitTimeMs;
  }

  @ApiModelProperty(required=false, value="automatically advance to the next target instance count after `deployStepWaitTimeMs` seconds")
  public Optional<Boolean> getAutoAdvanceDeploySteps() {
    return autoAdvanceDeploySteps;
  }

  @ApiModelProperty(required=false, value="allowed at most this many failed tasks to be retried before failing the deploy")
  public Optional<Integer> getMaxTaskRetries() {
    return maxTaskRetries;
  }

  @ApiModelProperty(required=false, value="Override the shell property on the mesos task")
  public Optional<Boolean> getShell() {
    return shell;
  }

  @ApiModelProperty(required=false, value="Run tasks as this user")
  public Optional<String> getUser() {
    return user;
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
        ", resources=" + resources +
        ", command=" + command +
        ", arguments=" + arguments +
        ", env=" + env +
        ", runImmediately" + runImmediatelyRequest +
        ", uris=" + uris +
        ", executorData=" + executorData +
        ", labels=" + labels +
        ", mesosLabels=" + mesosLabels +
        ", taskLabels=" + taskLabels +
        ", mesosTaskLabels=" + mesosTaskLabels +
        ", taskEnv=" + taskEnv +
        ", healthcheckUri=" + healthcheckUri +
        ", healthcheckIntervalSeconds=" + healthcheckIntervalSeconds +
        ", healthcheckTimeoutSeconds=" + healthcheckTimeoutSeconds +
        ", healthcheckPortIndex=" + healthcheckPortIndex +
        ", healthcheckProtocol=" + healthcheckProtocol +
        ", healthcheckMaxRetries=" + healthcheckMaxRetries +
        ", healthcheckMaxTotalTimeoutSeconds=" + healthcheckMaxTotalTimeoutSeconds +
        ", healthcheck=" + healthcheck +
        ", skipHealthchecksOnDeploy=" + skipHealthchecksOnDeploy +
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
