package com.hubspot.singularity;

import static com.hubspot.singularity.JsonHelpers.copyOfList;
import static com.hubspot.singularity.JsonHelpers.copyOfMap;
import static com.hubspot.singularity.JsonHelpers.copyOfSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.hubspot.deploy.ExecutorData;
import com.hubspot.deploy.HealthcheckOptions;
import com.hubspot.mesos.Resources;
import com.hubspot.mesos.SingularityContainerInfo;
import com.hubspot.mesos.SingularityMesosArtifact;
import com.hubspot.mesos.SingularityMesosTaskLabel;
import com.hubspot.singularity.api.SingularityRunNowRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Schema(
  description = "A set of instructions for launching tasks associated with a request"
)
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

  /**
   * @deprecated use {@link #mesosLabels}
   */
  @Deprecated
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

  @Deprecated
  /**
   * @deprecated use {@link CanaryDeploySettings} instead
   */
  private final Optional<Integer> deployInstanceCountPerStep;

  @Deprecated
  /**
   * @deprecated use {@link CanaryDeploySettings} instead
   */
  private final Optional<Integer> deployStepWaitTimeMs;

  @Deprecated
  /**
   * @deprecated use {@link CanaryDeploySettings} instead
   */
  private final Optional<Boolean> autoAdvanceDeploySteps;

  @Deprecated
  /**
   * @deprecated use {@link CanaryDeploySettings} instead
   */
  private final Optional<Integer> maxTaskRetries;

  private final Optional<Boolean> shell;
  private final Optional<String> user;
  private final List<SingularityS3UploaderFile> s3UploaderAdditionalFiles;
  private final CanaryDeploySettings canaryDeploySettings;

  public static SingularityDeployBuilder newBuilder(String requestId, String id) {
    return new SingularityDeployBuilder(requestId, id);
  }

  @JsonCreator
  public SingularityDeploy(
    @JsonProperty("requestId") String requestId,
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
    @JsonProperty(
      "runImmediately"
    ) Optional<SingularityRunNowRequest> runImmediatelyRequest,
    @JsonProperty("uris") Optional<List<SingularityMesosArtifact>> uris,
    @JsonProperty("metadata") Optional<Map<String, String>> metadata,
    @JsonProperty("executorData") Optional<ExecutorData> executorData,
    @JsonProperty("version") Optional<String> version,
    @JsonProperty("timestamp") Optional<Long> timestamp,
    @JsonProperty("labels") Optional<Map<String, String>> labels,
    @JsonProperty("mesosLabels") Optional<List<SingularityMesosTaskLabel>> mesosLabels,
    @JsonProperty("taskLabels") Optional<Map<Integer, Map<String, String>>> taskLabels,
    @JsonProperty(
      "mesosTaskLabels"
    ) Optional<Map<Integer, List<SingularityMesosTaskLabel>>> mesosTaskLabels,
    @JsonProperty("deployHealthTimeoutSeconds") Optional<Long> deployHealthTimeoutSeconds,
    @JsonProperty("healthcheckUri") Optional<String> healthcheckUri,
    @JsonProperty("healthcheckIntervalSeconds") Optional<Long> healthcheckIntervalSeconds,
    @JsonProperty("healthcheckTimeoutSeconds") Optional<Long> healthcheckTimeoutSeconds,
    @JsonProperty("healthcheckPortIndex") Optional<Integer> healthcheckPortIndex,
    @JsonProperty("healthcheckMaxRetries") Optional<Integer> healthcheckMaxRetries,
    @JsonProperty(
      "healthcheckMaxTotalTimeoutSeconds"
    ) Optional<Long> healthcheckMaxTotalTimeoutSeconds,
    @JsonProperty("healthcheck") Optional<HealthcheckOptions> healthcheck,
    @JsonProperty("serviceBasePath") Optional<String> serviceBasePath,
    @JsonProperty("loadBalancerGroups") Optional<Set<String>> loadBalancerGroups,
    @JsonProperty("loadBalancerPortIndex") Optional<Integer> loadBalancerPortIndex,
    @JsonProperty(
      "considerHealthyAfterRunningForSeconds"
    ) Optional<Long> considerHealthyAfterRunningForSeconds,
    @JsonProperty(
      "loadBalancerOptions"
    ) Optional<Map<String, Object>> loadBalancerOptions,
    @JsonProperty("loadBalancerDomains") Optional<Set<String>> loadBalancerDomains,
    @JsonProperty(
      "loadBalancerAdditionalRoutes"
    ) Optional<List<String>> loadBalancerAdditionalRoutes,
    @JsonProperty("loadBalancerTemplate") Optional<String> loadBalancerTemplate,
    @JsonProperty(
      "loadBalancerServiceIdOverride"
    ) Optional<String> loadBalancerServiceIdOverride,
    @JsonProperty("loadBalancerUpstreamGroup") Optional<String> loadBalancerUpstreamGroup,
    @JsonProperty("skipHealthchecksOnDeploy") Optional<Boolean> skipHealthchecksOnDeploy,
    @JsonProperty(
      "healthCheckProtocol"
    ) Optional<HealthcheckProtocol> healthcheckProtocol,
    @JsonProperty(
      "deployInstanceCountPerStep"
    ) Optional<Integer> deployInstanceCountPerStep,
    @JsonProperty("deployStepWaitTimeMs") Optional<Integer> deployStepWaitTimeMs,
    @JsonProperty("autoAdvanceDeploySteps") Optional<Boolean> autoAdvanceDeploySteps,
    @JsonProperty("maxTaskRetries") Optional<Integer> maxTaskRetries,
    @JsonProperty("shell") Optional<Boolean> shell,
    @JsonProperty("user") Optional<String> user,
    @JsonProperty(
      "s3UploaderAdditionalFiles"
    ) List<SingularityS3UploaderFile> s3UploaderAdditionalFiles,
    @JsonProperty(
      "canaryDeploySettings"
    ) Optional<CanaryDeploySettings> canaryDeploySettings
  ) {
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
    this.mesosLabels =
      mesosLabels.isPresent()
        ? mesosLabels
        : (labels.map(SingularityMesosTaskLabel::labelsFromMap));
    this.taskLabels = taskLabels;
    this.mesosTaskLabels =
      mesosTaskLabels.isPresent()
        ? mesosTaskLabels
        : (taskLabels.map(SingularityDeploy::parseMesosTaskLabelsFromMap));

    this.healthcheckUri = healthcheckUri;
    this.healthcheckIntervalSeconds = healthcheckIntervalSeconds;
    this.healthcheckTimeoutSeconds = healthcheckTimeoutSeconds;
    this.healthcheckPortIndex = healthcheckPortIndex;
    this.skipHealthchecksOnDeploy = skipHealthchecksOnDeploy;
    this.healthcheckProtocol = healthcheckProtocol;
    this.healthcheckMaxRetries = healthcheckMaxRetries;
    this.healthcheckMaxTotalTimeoutSeconds = healthcheckMaxTotalTimeoutSeconds;

    if (healthcheckUri.isPresent() && !healthcheck.isPresent()) {
      this.healthcheck =
        Optional.of(
          new HealthcheckOptions(
            healthcheckUri.get(),
            healthcheckPortIndex,
            Optional.empty(),
            healthcheckProtocol,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            healthcheckIntervalSeconds.map(Long::intValue),
            healthcheckTimeoutSeconds.map(Long::intValue),
            healthcheckMaxRetries,
            Optional.empty(),
            Optional.empty()
          )
        );
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
    if (!canaryDeploySettings.isPresent()) {
      if (deployInstanceCountPerStep.isPresent() || maxTaskRetries.isPresent()) {
        CanaryDeploySettingsBuilder builder = CanaryDeploySettings.newbuilder();
        builder.setEnableCanaryDeploy(deployInstanceCountPerStep.isPresent());
        deployInstanceCountPerStep.ifPresent(builder::setInstanceGroupSize);
        deployStepWaitTimeMs.ifPresent(w -> {
          builder.setAcceptanceMode(DeployAcceptanceMode.TIMED);
          builder.setWaitMillisBetweenGroups(w);
        });
        maxTaskRetries.ifPresent(builder::setAllowedTasksFailuresPerGroup);
        this.canaryDeploySettings = builder.build();
      } else {
        this.canaryDeploySettings = new CanaryDeploySettings();
      }
    } else {
      this.canaryDeploySettings = canaryDeploySettings.get();
    }
    this.maxTaskRetries = maxTaskRetries;

    this.shell = shell;
    this.user = user;
    this.s3UploaderAdditionalFiles =
      s3UploaderAdditionalFiles == null
        ? Collections.emptyList()
        : s3UploaderAdditionalFiles;
  }

  private static Map<Integer, List<SingularityMesosTaskLabel>> parseMesosTaskLabelsFromMap(
    Map<Integer, Map<String, String>> taskLabels
  ) {
    Map<Integer, List<SingularityMesosTaskLabel>> mesosTaskLabels = new HashMap<>();
    for (Map.Entry<Integer, Map<String, String>> entry : taskLabels.entrySet()) {
      mesosTaskLabels.put(
        entry.getKey(),
        SingularityMesosTaskLabel.labelsFromMap(entry.getValue())
      );
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
      .setUser(user)
      .setS3UploaderAdditionalFiles(s3UploaderAdditionalFiles)
      .setCanaryDeploySettings(canaryDeploySettings);
  }

  @Schema(
    nullable = true,
    description = "Number of seconds that Singularity waits for this service to become healthy (for it to download artifacts, start running, and optionally pass healthchecks)"
  )
  public Optional<Long> getDeployHealthTimeoutSeconds() {
    return deployHealthTimeoutSeconds;
  }

  @Schema(
    required = true,
    description = "Singularity Request Id which is associated with this deploy"
  )
  public String getRequestId() {
    return requestId;
  }

  @Schema(required = true, description = "Singularity deploy id")
  public String getId() {
    return id;
  }

  @Schema(nullable = true, description = "Deploy version")
  public Optional<String> getVersion() {
    return version;
  }

  @Schema(nullable = true, description = "Deploy timestamp")
  public Optional<Long> getTimestamp() {
    return timestamp;
  }

  @Schema(
    nullable = true,
    description = "Map of metadata key/value pairs associated with the deployment"
  )
  public Optional<Map<String, String>> getMetadata() {
    return metadata;
  }

  @Schema(
    nullable = true,
    description = "Container information for deployment into a container"
  )
  public Optional<SingularityContainerInfo> getContainerInfo() {
    return containerInfo;
  }

  @Schema(nullable = true, description = "Custom Mesos executor")
  public Optional<String> getCustomExecutorCmd() {
    return customExecutorCmd;
  }

  @Schema(nullable = true, description = "Custom Mesos executor id")
  public Optional<String> getCustomExecutorId() {
    return customExecutorId;
  }

  @Schema(nullable = true, description = "Custom Mesos executor source")
  public Optional<String> getCustomExecutorSource() {
    return customExecutorSource;
  }

  @Schema(
    nullable = true,
    description = "Resources to allocate for custom mesos executor"
  )
  public Optional<Resources> getCustomExecutorResources() {
    return customExecutorResources;
  }

  @Schema(nullable = true, description = "Resources required for this deploy")
  public Optional<Resources> getResources() {
    return resources;
  }

  @Schema(nullable = true, description = "Command to execute for this deployment")
  public Optional<String> getCommand() {
    return command;
  }

  @Schema(nullable = true, description = "Command arguments")
  public Optional<List<String>> getArguments() {
    return arguments;
  }

  @Schema(nullable = true, description = "Map of environment variable definitions")
  public Optional<Map<String, String>> getEnv() {
    return env;
  }

  @Schema(
    nullable = true,
    description = "Map of environment variable overrides for specific task instances (task instance number -> Map<String, String> of environment variables"
  )
  public Optional<Map<Integer, Map<String, String>>> getTaskEnv() {
    return taskEnv;
  }

  @Schema(
    nullable = true,
    description = "Settings used to run this deploy immediately (for non-long-running request types)"
  )
  public Optional<SingularityRunNowRequest> getRunImmediately() {
    return runImmediatelyRequest;
  }

  @Schema(
    nullable = true,
    description = "List of URIs to download before executing the deploy command"
  )
  public Optional<List<SingularityMesosArtifact>> getUris() {
    return uris;
  }

  @Schema(nullable = true, description = "Executor specific information")
  public Optional<ExecutorData> getExecutorData() {
    return executorData;
  }

  @Deprecated
  @Schema(
    nullable = true,
    description = "Deployment Healthcheck URI, if specified will be called after TASK_RUNNING"
  )
  public Optional<String> getHealthcheckUri() {
    return healthcheckUri;
  }

  @Deprecated
  @Schema(nullable = true, description = "Healthcheck protocol - HTTP or HTTPS")
  public Optional<HealthcheckProtocol> getHealthcheckProtocol() {
    return healthcheckProtocol;
  }

  @Deprecated
  @Schema(
    nullable = true,
    description = "Time to wait after a failed healthcheck to try again in seconds"
  )
  public Optional<Long> getHealthcheckIntervalSeconds() {
    return healthcheckIntervalSeconds;
  }

  @Deprecated
  @Schema(nullable = true, description = "Single healthcheck HTTP timeout in seconds")
  public Optional<Long> getHealthcheckTimeoutSeconds() {
    return healthcheckTimeoutSeconds;
  }

  @Deprecated
  @Schema(
    nullable = true,
    description = "Perform healthcheck on this dynamically allocated port (e.g. 0 for first port), defaults to first port"
  )
  public Optional<Integer> getHealthcheckPortIndex() {
    return healthcheckPortIndex;
  }

  @Schema(
    nullable = true,
    description = "The base path for the API exposed by the deploy. Used in conjunction with the Load balancer API"
  )
  public Optional<String> getServiceBasePath() {
    return serviceBasePath;
  }

  @Schema(
    nullable = true,
    description = "Number of seconds that a service must be healthy to consider the deployment to be successful"
  )
  public Optional<Long> getConsiderHealthyAfterRunningForSeconds() {
    return considerHealthyAfterRunningForSeconds;
  }

  @Schema(
    nullable = true,
    description = "List of load balancer groups associated with this deployment"
  )
  public Optional<Set<String>> getLoadBalancerGroups() {
    return loadBalancerGroups;
  }

  @Schema(
    nullable = true,
    description = "Send this port to the load balancer api (e.g. 0 for first port), defaults to first port"
  )
  public Optional<Integer> getLoadBalancerPortIndex() {
    return loadBalancerPortIndex;
  }

  @Schema(
    nullable = true,
    description = "Map (Key/Value) of options for the load balancer"
  )
  public Optional<Map<String, Object>> getLoadBalancerOptions() {
    return loadBalancerOptions;
  }

  @Schema(
    nullable = true,
    description = "List of domains to host this service on, for use with the load balancer api"
  )
  public Optional<Set<String>> getLoadBalancerDomains() {
    return loadBalancerDomains;
  }

  @Schema(
    nullable = true,
    description = "Additional routes besides serviceBasePath used by this service"
  )
  public Optional<List<String>> getLoadBalancerAdditionalRoutes() {
    return loadBalancerAdditionalRoutes;
  }

  @Schema(
    nullable = true,
    description = "Name of load balancer template to use if not using the default template"
  )
  public Optional<String> getLoadBalancerTemplate() {
    return loadBalancerTemplate;
  }

  @Schema(
    nullable = true,
    description = "Name of load balancer Service ID to use instead of the Request ID"
  )
  public Optional<String> getLoadBalancerServiceIdOverride() {
    return loadBalancerServiceIdOverride;
  }

  @Schema(
    nullable = true,
    description = "Group name to tag all upstreams with in load balancer"
  )
  public Optional<String> getLoadBalancerUpstreamGroup() {
    return loadBalancerUpstreamGroup;
  }

  @Deprecated
  @Schema(
    nullable = true,
    description = "Labels for all tasks associated with this deploy"
  )
  public Optional<Map<String, String>> getLabels() {
    return labels;
  }

  @Schema(
    nullable = true,
    description = "Labels for all tasks associated with this deploy"
  )
  public Optional<List<SingularityMesosTaskLabel>> getMesosLabels() {
    return mesosLabels;
  }

  @Schema(
    nullable = true,
    description = "(Deprecated) Labels for specific tasks associated with this deploy, indexed by instance number"
  )
  public Optional<Map<Integer, Map<String, String>>> getTaskLabels() {
    return taskLabels;
  }

  @Schema(
    nullable = true,
    description = "Labels for specific tasks associated with this deploy, indexed by instance number"
  )
  public Optional<Map<Integer, List<SingularityMesosTaskLabel>>> getMesosTaskLabels() {
    return mesosTaskLabels;
  }

  @Schema(
    nullable = true,
    description = "Allows skipping of health checks when deploying."
  )
  public Optional<Boolean> getSkipHealthchecksOnDeploy() {
    return skipHealthchecksOnDeploy;
  }

  @Deprecated
  @Schema(
    nullable = true,
    description = "Maximum number of times to retry an individual healthcheck before failing the deploy."
  )
  public Optional<Integer> getHealthcheckMaxRetries() {
    return healthcheckMaxRetries;
  }

  @Deprecated
  @Schema(
    nullable = true,
    description = "Maximum amount of time to wait before failing a deploy for healthchecks to pass."
  )
  public Optional<Long> getHealthcheckMaxTotalTimeoutSeconds() {
    return healthcheckMaxTotalTimeoutSeconds;
  }

  @Schema(description = "HTTP Healthcheck settings")
  public Optional<HealthcheckOptions> getHealthcheck() {
    return healthcheck;
  }

  @Deprecated
  /**
   * @deprecated use {@link CanaryDeploySettings} instead
   */
  @Schema(nullable = true, description = "deploy this many instances at a time")
  public Optional<Integer> getDeployInstanceCountPerStep() {
    return deployInstanceCountPerStep;
  }

  @Deprecated
  /**
   * @deprecated use {@link CanaryDeploySettings} instead
   */
  @Schema(nullable = true, description = "wait this long between deploy steps")
  public Optional<Integer> getDeployStepWaitTimeMs() {
    return deployStepWaitTimeMs;
  }

  @Deprecated
  /**
   * @deprecated use {@link CanaryDeploySettings} instead
   */
  @Schema(
    nullable = true,
    description = "automatically advance to the next target instance count after `deployStepWaitTimeMs` seconds"
  )
  public Optional<Boolean> getAutoAdvanceDeploySteps() {
    return autoAdvanceDeploySteps;
  }

  @Deprecated
  /**
   * @deprecated use {@link CanaryDeploySettings} instead
   */
  @Schema(
    nullable = true,
    description = "allowed at most this many failed tasks to be retried before failing the deploy"
  )
  public Optional<Integer> getMaxTaskRetries() {
    return maxTaskRetries;
  }

  @Schema(nullable = true, description = "Override the shell property on the mesos task")
  public Optional<Boolean> getShell() {
    return shell;
  }

  @Schema(nullable = true, description = "Run tasks as this user")
  public Optional<String> getUser() {
    return user;
  }

  @Schema(
    description = "Specify additional sandbox files to upload to S3 for this deploy"
  )
  public List<SingularityS3UploaderFile> getS3UploaderAdditionalFiles() {
    return s3UploaderAdditionalFiles;
  }

  @Schema(
    description = "A set of instructions for how to roll out groups of instances for a new deploy. Takes precedence over the deprecated deployStep... settings"
  )
  public CanaryDeploySettings getCanaryDeploySettings() {
    return canaryDeploySettings;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityDeploy that = (SingularityDeploy) o;
    return (
      Objects.equals(requestId, that.requestId) &&
      Objects.equals(id, that.id) &&
      Objects.equals(version, that.version) &&
      Objects.equals(timestamp, that.timestamp) &&
      Objects.equals(metadata, that.metadata) &&
      Objects.equals(containerInfo, that.containerInfo) &&
      Objects.equals(customExecutorCmd, that.customExecutorCmd) &&
      Objects.equals(customExecutorId, that.customExecutorId) &&
      Objects.equals(customExecutorSource, that.customExecutorSource) &&
      Objects.equals(customExecutorResources, that.customExecutorResources) &&
      Objects.equals(resources, that.resources) &&
      Objects.equals(command, that.command) &&
      Objects.equals(arguments, that.arguments) &&
      Objects.equals(env, that.env) &&
      Objects.equals(uris, that.uris) &&
      Objects.equals(executorData, that.executorData) &&
      Objects.equals(labels, that.labels) &&
      Objects.equals(mesosLabels, that.mesosLabels) &&
      Objects.equals(taskLabels, that.taskLabels) &&
      Objects.equals(mesosTaskLabels, that.mesosTaskLabels) &&
      Objects.equals(taskEnv, that.taskEnv) &&
      Objects.equals(runImmediatelyRequest, that.runImmediatelyRequest) &&
      Objects.equals(healthcheckUri, that.healthcheckUri) &&
      Objects.equals(healthcheckIntervalSeconds, that.healthcheckIntervalSeconds) &&
      Objects.equals(healthcheckTimeoutSeconds, that.healthcheckTimeoutSeconds) &&
      Objects.equals(healthcheckPortIndex, that.healthcheckPortIndex) &&
      Objects.equals(healthcheckProtocol, that.healthcheckProtocol) &&
      Objects.equals(healthcheckMaxRetries, that.healthcheckMaxRetries) &&
      Objects.equals(
        healthcheckMaxTotalTimeoutSeconds,
        that.healthcheckMaxTotalTimeoutSeconds
      ) &&
      Objects.equals(healthcheck, that.healthcheck) &&
      Objects.equals(skipHealthchecksOnDeploy, that.skipHealthchecksOnDeploy) &&
      Objects.equals(deployHealthTimeoutSeconds, that.deployHealthTimeoutSeconds) &&
      Objects.equals(
        considerHealthyAfterRunningForSeconds,
        that.considerHealthyAfterRunningForSeconds
      ) &&
      Objects.equals(serviceBasePath, that.serviceBasePath) &&
      Objects.equals(loadBalancerGroups, that.loadBalancerGroups) &&
      Objects.equals(loadBalancerPortIndex, that.loadBalancerPortIndex) &&
      Objects.equals(loadBalancerOptions, that.loadBalancerOptions) &&
      Objects.equals(loadBalancerDomains, that.loadBalancerDomains) &&
      Objects.equals(loadBalancerAdditionalRoutes, that.loadBalancerAdditionalRoutes) &&
      Objects.equals(loadBalancerTemplate, that.loadBalancerTemplate) &&
      Objects.equals(loadBalancerServiceIdOverride, that.loadBalancerServiceIdOverride) &&
      Objects.equals(loadBalancerUpstreamGroup, that.loadBalancerUpstreamGroup) &&
      Objects.equals(deployInstanceCountPerStep, that.deployInstanceCountPerStep) &&
      Objects.equals(deployStepWaitTimeMs, that.deployStepWaitTimeMs) &&
      Objects.equals(autoAdvanceDeploySteps, that.autoAdvanceDeploySteps) &&
      Objects.equals(maxTaskRetries, that.maxTaskRetries) &&
      Objects.equals(shell, that.shell) &&
      Objects.equals(user, that.user) &&
      Objects.equals(s3UploaderAdditionalFiles, that.s3UploaderAdditionalFiles) &&
      Objects.equals(canaryDeploySettings, that.canaryDeploySettings)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      requestId,
      id,
      version,
      timestamp,
      metadata,
      containerInfo,
      customExecutorCmd,
      customExecutorId,
      customExecutorSource,
      customExecutorResources,
      resources,
      command,
      arguments,
      env,
      uris,
      executorData,
      labels,
      mesosLabels,
      taskLabels,
      mesosTaskLabels,
      taskEnv,
      runImmediatelyRequest,
      healthcheckUri,
      healthcheckIntervalSeconds,
      healthcheckTimeoutSeconds,
      healthcheckPortIndex,
      healthcheckProtocol,
      healthcheckMaxRetries,
      healthcheckMaxTotalTimeoutSeconds,
      healthcheck,
      skipHealthchecksOnDeploy,
      deployHealthTimeoutSeconds,
      considerHealthyAfterRunningForSeconds,
      serviceBasePath,
      loadBalancerGroups,
      loadBalancerPortIndex,
      loadBalancerOptions,
      loadBalancerDomains,
      loadBalancerAdditionalRoutes,
      loadBalancerTemplate,
      loadBalancerServiceIdOverride,
      loadBalancerUpstreamGroup,
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
      .add("uris", uris)
      .add("executorData", executorData)
      .add("labels", labels)
      .add("mesosLabels", mesosLabels)
      .add("taskLabels", taskLabels)
      .add("mesosTaskLabels", mesosTaskLabels)
      .add("taskEnv", taskEnv)
      .add("runImmediatelyRequest", runImmediatelyRequest)
      .add("healthcheckUri", healthcheckUri)
      .add("healthcheckIntervalSeconds", healthcheckIntervalSeconds)
      .add("healthcheckTimeoutSeconds", healthcheckTimeoutSeconds)
      .add("healthcheckPortIndex", healthcheckPortIndex)
      .add("healthcheckProtocol", healthcheckProtocol)
      .add("healthcheckMaxRetries", healthcheckMaxRetries)
      .add("healthcheckMaxTotalTimeoutSeconds", healthcheckMaxTotalTimeoutSeconds)
      .add("healthcheck", healthcheck)
      .add("skipHealthchecksOnDeploy", skipHealthchecksOnDeploy)
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
