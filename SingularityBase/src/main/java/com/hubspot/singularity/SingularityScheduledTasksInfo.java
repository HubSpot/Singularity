package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class SingularityScheduledTasksInfo {
  private final int numFutureTasks;
  private final long maxTaskLag;
  private final long timestamp;
  private final List<SingularityPendingTaskId> lateTasks;
  private final List<SingularityPendingTaskId> onDemandLateTasks;

  @JsonCreator
  public SingularityScheduledTasksInfo(
    @JsonProperty("lateTasks") List<SingularityPendingTaskId> lateTasks,
    @JsonProperty("onDemandLateTasks") List<SingularityPendingTaskId> onDemandLateTasks,
    @JsonProperty("numFutureTasks") int numFutureTasks,
    @JsonProperty("maxTaskLag") long maxTaskLag,
    @JsonProperty("timestamp") long timestamp
  ) {
    this.lateTasks = lateTasks;
    this.onDemandLateTasks = onDemandLateTasks;
    this.numFutureTasks = numFutureTasks;
    this.maxTaskLag = maxTaskLag;
    this.timestamp = timestamp;
  }

  public List<SingularityPendingTaskId> getLateTasks() {
    return lateTasks;
  }

  public List<SingularityPendingTaskId> getOnDemandLateTasks() {
    return onDemandLateTasks;
  }

  public int getNumFutureTasks() {
    return numFutureTasks;
  }

  public long getMaxTaskLag() {
    return maxTaskLag;
  }

  public long getTimestamp() {
    return timestamp;
  }
}
