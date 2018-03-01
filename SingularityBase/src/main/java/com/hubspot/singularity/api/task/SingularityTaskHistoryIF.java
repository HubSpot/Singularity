package com.hubspot.singularity.api.task;

import java.util.List;
import java.util.Optional;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.annotations.SingularityStyle;
import com.hubspot.singularity.api.common.SingularityLoadBalancerUpdate;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema(description = "Describes the full history of a Singularity task")
public interface SingularityTaskHistoryIF {
  @Schema(description = "A list of status updates for this task")
  List<SingularityTaskHistoryUpdate> getTaskUpdates();

  @Schema(description = "The directory of the task sandbox on teh mesos slave", nullable = true)
  Optional<String> getDirectory();

  @Schema(description = "If a docker task, the docker container id", nullable = true)
  Optional<String> getContainerId();

  @Schema(description = "Healthcheck results for this task")
  List<SingularityTaskHealthcheckResult> getHealthcheckResults();

  @Schema(description = "Full Singularity task data")
  SingularityTask getTask();

  @Schema(description = "A list of load balancer updates for this task")
  List<SingularityLoadBalancerUpdate> getLoadBalancerUpdates();

  @Schema(description = "A list of shell commands that have been run against this task")
  List<SingularityTaskShellCommandHistory> getShellCommandHistory();

  @Schema(description = "A list of custom metadata associated with this task")
  List<SingularityTaskMetadata> getTaskMetadata();

  @JsonIgnore
  default Optional<SingularityTaskHistoryUpdate> getLastTaskUpdate() {
    return JavaUtils.getLast(getTaskUpdates());
  }
}
