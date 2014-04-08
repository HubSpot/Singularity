package com.hubspot.singularity;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

public class SingularityTaskHistory extends SingularityJsonObject {

  private final List<SingularityTaskHistoryUpdate> taskUpdates;
  private final Optional<String> directory;
  private final Optional<SingularityTaskHealthcheckResult> lastHealthcheck;
  private final SingularityTask task;
  private final Optional<LoadBalancerState> addLoadBalancerState;
  private final Optional<LoadBalancerState> removeLoadBalancerState;
  
  public static SingularityTaskHistory fromBytes(byte[] bytes, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(bytes, SingularityTaskHistory.class);
    } catch (IOException e) {
      throw new SingularityJsonException(e);
    }
  }
  
  @JsonCreator
  public SingularityTaskHistory(@JsonProperty("taskUpdates") List<SingularityTaskHistoryUpdate> taskUpdates, @JsonProperty("directory") Optional<String> directory, @JsonProperty("lastHealthcheck") Optional<SingularityTaskHealthcheckResult> lastHealthcheck, 
      @JsonProperty("task") SingularityTask task, @JsonProperty("addLoadBalancerState") Optional<LoadBalancerState> addLoadBalancerState, @JsonProperty("removeLoadBalancerState") Optional<LoadBalancerState> removeLoadBalancerState) {
    this.taskUpdates = taskUpdates;
    this.lastHealthcheck = lastHealthcheck;
    this.directory = directory;
    this.task = task;
    this.addLoadBalancerState = addLoadBalancerState;
    this.removeLoadBalancerState = removeLoadBalancerState;
  }

  public List<SingularityTaskHistoryUpdate> getTaskUpdates() {
    return taskUpdates;
  }
  
  public Optional<SingularityTaskHealthcheckResult> getLastHealthcheck() {
    return lastHealthcheck;
  }
  
  public Optional<LoadBalancerState> getAddLoadBalancerState() {
    return addLoadBalancerState;
  }

  public Optional<LoadBalancerState> getRemoveLoadBalancerState() {
    return removeLoadBalancerState;
  }

  public Optional<String> getDirectory() {
    return directory;
  }

  public SingularityTask getTask() {
    return task;
  }

  @Override
  public String toString() {
    return "SingularityTaskHistory [taskUpdates=" + taskUpdates + ", directory=" + directory + ", lastHealthcheck=" + lastHealthcheck + ", task=" + task + ", addLoadBalancerState=" + addLoadBalancerState + ", removeLoadBalancerState="
        + removeLoadBalancerState + "]";
  }

}
