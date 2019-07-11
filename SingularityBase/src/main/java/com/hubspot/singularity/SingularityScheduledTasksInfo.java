package com.hubspot.singularity;

import java.util.List;

public class SingularityScheduledTasksInfo {

  private final int numFutureTasks;
  private final long maxTaskLag;
  private final long timestamp;
  private final List<SingularityPendingTaskId> lateTasks;
  private final List<SingularityPendingTaskId> onDemandLateTasks;

  public SingularityScheduledTasksInfo(List<SingularityPendingTaskId> lateTasks, List<SingularityPendingTaskId> onDemandLateTasks, int numFutureTasks, long maxTaskLag, long timestamp) {
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
