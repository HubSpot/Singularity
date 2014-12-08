package com.hubspot.singularity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityTaskHistory {

  private final List<SingularityTaskHistoryUpdate> taskUpdates;
  private final Optional<String> directory;
  private final SingularityTask task;
  private final List<SingularityTaskHealthcheckResult> healthcheckResults;
  private final List<SingularityLoadBalancerUpdate> loadBalancerUpdates;

  @JsonCreator
  public SingularityTaskHistory(@JsonProperty("taskUpdates") List<SingularityTaskHistoryUpdate> taskUpdates, @JsonProperty("directory") Optional<String> directory, @JsonProperty("healthcheckResults") List<SingularityTaskHealthcheckResult> healthcheckResults,
      @JsonProperty("task") SingularityTask task, @JsonProperty("loadBalancerUpdates") List<SingularityLoadBalancerUpdate> loadBalancerUpdates) {
    this.taskUpdates = taskUpdates;
    this.healthcheckResults = healthcheckResults;
    this.directory = directory;
    this.task = task;
    this.loadBalancerUpdates = loadBalancerUpdates;
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

  public List<SingularityTaskHealthcheckResult> getHealthcheckResults() {
    return healthcheckResults;
  }

  public List<SingularityLoadBalancerUpdate> getLoadBalancerUpdates() {
    return loadBalancerUpdates;
  }

  @Override
  public String toString() {
    return "SingularityTaskHistory [taskUpdates=" + taskUpdates + ", directory=" + directory + ", task=" + task + ", healthcheckResults=" + healthcheckResults + ", loadBalancerUpdates=" + loadBalancerUpdates + "]";
  }

}
