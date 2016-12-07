package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;

public class SingularityMailDisasterDataPoint {
  private final String time;
  private final int numActiveTasks;
  private final int numPendingTasks;
  private final int numLateTasks;
  private final long avgTaskLagMillis;
  private final int numLostTasks;
  private final int numLostSlaves;

  @JsonCreator
  public SingularityMailDisasterDataPoint(String humanizedTimestamp, SingularityDisasterDataPoint dataPoint) {
    this.time = humanizedTimestamp;
    this.numActiveTasks = dataPoint.getNumActiveTasks();
    this.numPendingTasks = dataPoint.getNumPendingTasks();
    this.numLateTasks = dataPoint.getNumLateTasks();
    this.avgTaskLagMillis = dataPoint.getAvgTaskLagMillis();
    this.numLostTasks = dataPoint.getNumLostTasks();
    this.numLostSlaves = dataPoint.getNumLostSlaves();
  }

  public String getTime() {
    return time;
  }

  public int getNumActiveTasks() {
    return numActiveTasks;
  }

  public int getNumPendingTasks() {
    return numPendingTasks;
  }

  public int getNumLateTasks() {
    return numLateTasks;
  }

  public long getAvgTaskLagMillis() {
    return avgTaskLagMillis;
  }

  public int getNumLostTasks() {
    return numLostTasks;
  }

  public int getNumLostSlaves() {
    return numLostSlaves;
  }
}
