package com.hubspot.singularity;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Optional;
import com.hubspot.deploy.ExecutorData;
import com.hubspot.deploy.HealthcheckOptions;
import com.hubspot.immutables.style.SingularityStyle;
import com.hubspot.mesos.Resources;
import com.hubspot.mesos.SingularityContainerInfo;
import com.hubspot.mesos.SingularityMesosArtifact;
import com.hubspot.mesos.SingularityMesosTaskLabel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Immutable(prehash = true)
@SingularityStyle
public abstract class AbstractSingularityDeploy {

  private static Map<Integer, List<SingularityMesosTaskLabel>> parseMesosTaskLabelsFromMap(Map<Integer, Map<String, String>> taskLabels) {
    Map<Integer, List<SingularityMesosTaskLabel>> mesosTaskLabels = new HashMap<>();
    for (Map.Entry<Integer, Map<String, String>> entry : taskLabels.entrySet()) {
      mesosTaskLabels.put(entry.getKey(), SingularityMesosTaskLabel.labelsFromMap(entry.getValue()));
    }
    return mesosTaskLabels;
  }

  @ApiModelProperty(required=false, value="Number of seconds that Singularity waits for this service to become healthy (for it to download artifacts, start running, and optionally pass healthchecks.)")
  public abstract Optional<Long> getDeployHealthTimeoutSeconds();

  @ApiModelProperty(required=true, value="Singularity Request Id which is associated with this deploy.")
  public abstract String getRequestId();

  @ApiModelProperty(required=true, value="Singularity deploy id.")
  public abstract String getId();

  @ApiModelProperty(required=false, value="Deploy version")
  public abstract Optional<String> getVersion();

  @ApiModelProperty(required=false, value="Deploy timestamp.")
  public abstract Optional<Long> getTimestamp();

  @ApiModelProperty(required=false, value="Map of metadata key/value pairs associated with the deployment.")
  public abstract Optional<Map<String, String>> getMetadata();

  @ApiModelProperty(required=false, value="Container information for deployment into a container.", dataType="SingularityContainerInfo")
  public abstract Optional<SingularityContainerInfo> getContainerInfo();

  @ApiModelProperty(required=false, value="Custom Mesos executor", dataType="string")
  public abstract Optional<String> getCustomExecutorCmd();

  @ApiModelProperty(required=false, value="Custom Mesos executor id.", dataType="string")
  public abstract Optional<String> getCustomExecutorId();

  @ApiModelProperty(required=false, value="Custom Mesos executor source.", dataType="string")
  public abstract Optional<String> getCustomExecutorSource();

  @ApiModelProperty(required=false, value="Resources to alate for custom mesos executor", dataType="com.hubspot.mesos.Resources")
  public abstract Optional<Resources> getCustomExecutorResources();

  @ApiModelProperty(required=false, value="Resources required for this deploy.", dataType="com.hubspot.mesos.Resources")
  public abstract Optional<Resources> getResources();

  @ApiModelProperty(required=false, value="Command to execute for this deployment.")
  public abstract Optional<String> getCommand();

  @ApiModelProperty(required=false, value="Command arguments.")
  public abstract Optional<List<String>> getArguments();

  @ApiModelProperty(required=false, value="Map of environment variable definitions.")
  public abstract Optional<Map<String, String>> getEnv();

  @ApiModelProperty(required=false, value="Map of environment variable overrides for specific task instances.")
  public abstract Optional<Map<Integer, Map<String, String>>> getTaskEnv();

  @ApiModelProperty(required=false, value="List of URIs to download before executing the deploy command.")
  public abstract Optional<List<SingularityMesosArtifact>> getUris();

  @ApiModelProperty(required=false, value="Executor specific information")
  public abstract Optional<ExecutorData> getExecutorData();

  /**
   * @deprecated use {@link #healthcheck}
   */
  @Deprecated
  @ApiModelProperty(required=false, value="Deployment Healthcheck URI, if specified will be called after TASK_RUNNING.")
  public abstract Optional<String> getHealthcheckUri();

  /**
   * @deprecated use {@link #healthcheck}
   */
  @Deprecated
  @ApiModelProperty(required=false, value="Healthcheck protocol - HTTP or HTTPS", dataType="com.hubspot.singularity.HealthcheckProtocol")
  public abstract Optional<HealthcheckProtocol> getHealthcheckProtocol();

  /**
   * @deprecated use {@link #healthcheck}
   */
  @Deprecated
  @ApiModelProperty(required=false, value="Time to wait after a failed healthcheck to try again in seconds.")
  public abstract Optional<Long> getHealthcheckIntervalSeconds();

  /**
   * @deprecated use {@link #healthcheck}
   */
  @Deprecated
  @ApiModelProperty(required=false, value="Single healthcheck HTTP timeout in seconds.")
  public abstract Optional<Long> getHealthcheckTimeoutSeconds();

  /**
   * @deprecated use {@link #healthcheck}
   */
  @Deprecated
  @ApiModelProperty(required=false, value="Perform healthcheck on this dynamically allocated port (e.g. 0 for first port), defaults to first port")
  public abstract Optional<Integer> getHealthcheckPortIndex();

  @ApiModelProperty(required=false, value="The base path for the API exposed by the deploy. Used in conjunction with the Load balancer API.")
  public abstract Optional<String> getServiceBasePath();

  @ApiModelProperty(required=false, value="Number of seconds that a service must be healthy to consider the deployment to be successful.")
  public abstract Optional<Long> getConsiderHealthyAfterRunningForSeconds();

  @ApiModelProperty(required=false, value="List of load balancer groups associated with this deployment.")
  public abstract Optional<Set<String>> getLoadBalancerGroups();

  @ApiModelProperty(required=false, value="Send this port to the load balancer api (e.g. 0 for first port), defaults to first port")
  public abstract Optional<Integer> getLoadBalancerPortIndex();

  @ApiModelProperty(required=false, value="Map (Key/Value) of options for the load balancer.")
  public abstract Optional<Map<String, Object>> getLoadBalancerOptions();

  @ApiModelProperty(required=false, value="List of domains to host this service on, for use with the load balancer api")
  public abstract Optional<Set<String>> getLoadBalancerDomains();

  @ApiModelProperty(required=false, value="Additional routes besides serviceBasePath used by this service")
  public abstract Optional<List<String>> getLoadBalancerAdditionalRoutes();

  @ApiModelProperty(required=false, value="Name of load balancer template to use if not using the default template")
  public abstract Optional<String> getLoadBalancerTemplate();

  @ApiModelProperty(required=false, value="Name of load balancer Service ID to use instead of the Request ID")
  public abstract Optional<String> getLoadBalancerServiceIdOverride();

  @ApiModelProperty(required=false, value="Group name to tag all upstreams with in load balancer")
  public abstract Optional<String> getLoadBalancerUpstreamGroup();

  @Deprecated
  @ApiModelProperty(required=false, value="Labels for all tasks associated with this deploy")
  public abstract Optional<Map<String, String>> getLabels();

  @ApiModelProperty(required=false, value="Labels for all tasks associated with this deploy")
  public abstract Optional<List<SingularityMesosTaskLabel>> getMesosLabels();

  @Deprecated
  @ApiModelProperty(required=false, value="(Deprecated) Labels for specific tasks associated with this deploy, indexed by instance number")
  public abstract Optional<Map<Integer, Map<String, String>>> getTaskLabels();

  @ApiModelProperty(required=false, value="Labels for specific tasks associated with this deploy, indexed by instance number")
  public abstract Optional<Map<Integer, List<SingularityMesosTaskLabel>>> getMesosTaskLabels();

  @ApiModelProperty(required=false, value="Allows skipping of health checks when deploying.")
  public abstract Optional<Boolean> getSkipHealthchecksOnDeploy();

  /**
   * @deprecated use {@link #healthcheck}
   */
  @Deprecated
  @ApiModelProperty(required=false, value="Maximum number of times to retry an individual healthcheck before failing the deploy.")
  public abstract Optional<Integer> getHealthcheckMaxRetries();

  /**
   * @deprecated use {@link #healthcheck}
   */
  @Deprecated
  @ApiModelProperty(required=false, value="Maximum amount of time to wait before failing a deploy for healthchecks to pass.")
  public abstract Optional<Long> getHealthcheckMaxTotalTimeoutSeconds();

  @ApiModelProperty(required = false, value="HTTP Healthcheck settings")
  public abstract Optional<HealthcheckOptions> getHealthcheck();

  @ApiModelProperty(required=false, value="deploy this many instances at a time")
  public abstract Optional<Integer> getDeployInstanceCountPerStep();

  @ApiModelProperty(required=false, value="wait this long between deploy steps")
  public abstract Optional<Integer> getDeployStepWaitTimeMs();

  @ApiModelProperty(required=false, value="automatically advance to the next target instance count after `deployStepWaitTimeMs` seconds")
  public abstract Optional<Boolean> getAutoAdvanceDeploySteps();

  @ApiModelProperty(required=false, value="allowed at most this many failed tasks to be retried before failing the deploy")
  public abstract Optional<Integer> getMaxTaskRetries();

  @ApiModelProperty(required=false, value="Override the shell property on the mesos task")
  public abstract Optional<Boolean> getShell();

  @ApiModelProperty(required=false, value="Run tasks as this user")
  public abstract Optional<String> getUser();

  @Derived
  @JsonIgnore
  public Optional<Map<Integer, List<SingularityMesosTaskLabel>>> getValidatedTaskLabels() {
    return getMesosTaskLabels().or(getTaskLabels().isPresent() ? Optional.of(parseMesosTaskLabelsFromMap(getTaskLabels().get())) : Optional.absent());
  }

  @Derived
  @JsonIgnore
  public Optional<List<SingularityMesosTaskLabel>> getValidatedLabels() {
    return getMesosLabels().or(getLabels().isPresent() ? Optional.of(SingularityMesosTaskLabel.labelsFromMap(getLabels().get())) : Optional.<List<SingularityMesosTaskLabel>>absent());
  }

  @Derived
  @JsonIgnore
  public Optional<HealthcheckOptions> getValidatedHealthcheckOptions() {
    if (getHealthcheckUri().isPresent() && !getHealthcheck().isPresent()) {
      return Optional.of(new HealthcheckOptions(
          getHealthcheckUri().get(),
          getHealthcheckPortIndex(),
          Optional.absent(),
          getHealthcheckProtocol(),
          Optional.absent(),
          Optional.absent(),
          Optional.absent(),
          getHealthcheckIntervalSeconds().isPresent() ? Optional.of(getHealthcheckIntervalSeconds().get().intValue()) : Optional.absent(),
          getHealthcheckTimeoutSeconds().isPresent() ? Optional.of(getHealthcheckTimeoutSeconds().get().intValue()) : Optional.absent(),
          getHealthcheckMaxRetries(),
          Collections.emptyList()));
    } else {
      return getHealthcheck();
    }
  }
}
