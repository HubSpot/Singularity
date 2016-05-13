package com.hubspot.singularity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.mesos.JavaUtils;

public class SingularityTaskHistory {

  private final List<SingularityTaskHistoryUpdate> taskUpdates;
  private final Optional<String> directory;
  private final SingularityTask task;
  private final List<SingularityTaskHealthcheckResult> healthcheckResults;
  private final List<SingularityLoadBalancerUpdate> loadBalancerUpdates;
  private final List<SingularityTaskShellCommandHistory> shellCommandHistory;
  private final List<SingularityTaskMetadata> taskMetadata;

  @JsonCreator
  public SingularityTaskHistory(@JsonProperty("taskUpdates") List<SingularityTaskHistoryUpdate> taskUpdates, @JsonProperty("directory") Optional<String> directory,
      @JsonProperty("healthcheckResults") List<SingularityTaskHealthcheckResult> healthcheckResults, @JsonProperty("task") SingularityTask task,
      @JsonProperty("loadBalancerUpdates") List<SingularityLoadBalancerUpdate> loadBalancerUpdates,
      @JsonProperty("shellCommandHistory") List<SingularityTaskShellCommandHistory> shellCommandHistory,
      @JsonProperty("taskMetadata") List<SingularityTaskMetadata> taskMetadata) {
    this.directory = directory;
    this.task = task;
    this.taskUpdates = JavaUtils.nonNullImmutable(taskUpdates);
    this.healthcheckResults = JavaUtils.nonNullImmutable(healthcheckResults);
    this.loadBalancerUpdates = JavaUtils.nonNullImmutable(loadBalancerUpdates);
    this.shellCommandHistory = JavaUtils.nonNullImmutable(shellCommandHistory);
    this.taskMetadata = JavaUtils.nonNullImmutable(taskMetadata);
  }

  public List<SingularityTaskHistoryUpdate> getTaskUpdates() {
    return taskUpdates;
  }

  public Optional<String> getDirectory() {
    return directory;
  }

  public SingularityTask getTask() {
    return task;
  }

  public List<SingularityTaskMetadata> getTaskMetadata() {
    return taskMetadata;
  }

  public List<SingularityTaskHealthcheckResult> getHealthcheckResults() {
    return healthcheckResults;
  }

  public List<SingularityLoadBalancerUpdate> getLoadBalancerUpdates() {
    return loadBalancerUpdates;
  }

  public List<SingularityTaskShellCommandHistory> getShellCommandHistory() {
    return shellCommandHistory;
  }

  @JsonIgnore
  public Optional<SingularityTaskHistoryUpdate> getLastTaskUpdate() {
    return JavaUtils.getLast(getTaskUpdates());
  }

  @Override
  public String toString() {
    return "SingularityTaskHistory [taskUpdates=" + taskUpdates + ", directory=" + directory + ", task=" + task + ", healthcheckResults=" + healthcheckResults + ", loadBalancerUpdates="
        + loadBalancerUpdates + ", shellCommandHistory=" + shellCommandHistory + ", taskMetadata=" + taskMetadata + "]";
  }

}
