package com.hubspot.singularity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.mesos.JavaUtils;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Describes the full history of a Singularity task")
public class SingularityTaskHistory {

  private final List<SingularityTaskHistoryUpdate> taskUpdates;
  private final Optional<String> directory;
  private final Optional<String> containerId;
  private final SingularityTask task;
  private final List<SingularityTaskHealthcheckResult> healthcheckResults;
  private final List<SingularityLoadBalancerUpdate> loadBalancerUpdates;
  private final List<SingularityTaskShellCommandHistory> shellCommandHistory;
  private final List<SingularityTaskMetadata> taskMetadata;

  @JsonCreator
  public SingularityTaskHistory(@JsonProperty("taskUpdates") List<SingularityTaskHistoryUpdate> taskUpdates,
                                @JsonProperty("directory") Optional<String> directory,
                                @JsonProperty("containerId") Optional<String> containerId,
                                @JsonProperty("healthcheckResults") List<SingularityTaskHealthcheckResult> healthcheckResults,
                                @JsonProperty("task") SingularityTask task,
                                @JsonProperty("loadBalancerUpdates") List<SingularityLoadBalancerUpdate> loadBalancerUpdates,
                                @JsonProperty("shellCommandHistory") List<SingularityTaskShellCommandHistory> shellCommandHistory,
                                @JsonProperty("taskMetadata") List<SingularityTaskMetadata> taskMetadata) {
    this.directory = directory;
    this.containerId = containerId;
    this.task = task;
    this.taskUpdates = JavaUtils.nonNullImmutable(taskUpdates);
    this.healthcheckResults = JavaUtils.nonNullImmutable(healthcheckResults);
    this.loadBalancerUpdates = JavaUtils.nonNullImmutable(loadBalancerUpdates);
    this.shellCommandHistory = JavaUtils.nonNullImmutable(shellCommandHistory);
    this.taskMetadata = JavaUtils.nonNullImmutable(taskMetadata);
  }

  @Schema(description = "A list of status updates for this task")
  public List<SingularityTaskHistoryUpdate> getTaskUpdates() {
    return taskUpdates;
  }

  @Schema(description = "The directory of the task sandbox on teh mesos slave", nullable = true)
  public Optional<String> getDirectory() {
    return directory;
  }

  @Schema(description = "If a docker task, the docker container id", nullable = true)
  public Optional<String> getContainerId() {
    return containerId;
  }

  @Schema(description = "Full Singularity task data")
  public SingularityTask getTask() {
    return task;
  }

  @Schema(description = "A list of custom metadata associated with this task")
  public List<SingularityTaskMetadata> getTaskMetadata() {
    return taskMetadata;
  }

  @Schema(description = "Healthcheck results for this task")
  public List<SingularityTaskHealthcheckResult> getHealthcheckResults() {
    return healthcheckResults;
  }

  @Schema(description = "A list of load balancer updates for this task")
  public List<SingularityLoadBalancerUpdate> getLoadBalancerUpdates() {
    return loadBalancerUpdates;
  }

  @Schema(description = "A list of shell commands that have been run against this task")
  public List<SingularityTaskShellCommandHistory> getShellCommandHistory() {
    return shellCommandHistory;
  }

  @JsonIgnore
  public Optional<SingularityTaskHistoryUpdate> getLastTaskUpdate() {
    return JavaUtils.getLast(getTaskUpdates());
  }

  @Override
  public String toString() {
    return "SingularityTaskHistory{" +
        "taskUpdates=" + taskUpdates +
        ", directory=" + directory +
        ", containerId=" + containerId +
        ", task=" + task +
        ", healthcheckResults=" + healthcheckResults +
        ", loadBalancerUpdates=" + loadBalancerUpdates +
        ", shellCommandHistory=" + shellCommandHistory +
        ", taskMetadata=" + taskMetadata +
        '}';
  }
}
