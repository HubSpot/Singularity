package com.hubspot.singularity;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

public class SingularityDisasterStats {
  private final long timestamp;
  private final int numActiveTasks;
  private final int numPendingTasks;
  private final int numOverdueTasks;
  private final long avgTaskLagMillis;
  private final int numActiveSlaves;
  private final int numLostSlaves;

  @JsonCreator
  public SingularityDisasterStats(@JsonProperty("timestamp") long timestamp,
                                  @JsonProperty("numActiveTasks") int numActiveTasks,
                                  @JsonProperty("numPendingTasks") int numPendingTasks,
                                  @JsonProperty("numOverdueTasks") int numOverdueTasks,
                                  @JsonProperty("avgTaskLagMillis") long avgTaskLagMillis,
                                  @JsonProperty("numActiveSlaves") int numActiveSlaves,
                                  @JsonProperty("numLostSlaves") int numLostSlaves) {
    this.timestamp = timestamp;
    this.numActiveTasks = numActiveTasks;
    this.numPendingTasks = numPendingTasks;
    this.numOverdueTasks = numOverdueTasks;
    this.avgTaskLagMillis = avgTaskLagMillis;
    this.numActiveSlaves = numActiveSlaves;
    this.numLostSlaves = numLostSlaves;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public int getNumActiveTasks() {
    return numActiveTasks;
  }

  public int getNumPendingTasks() {
    return numPendingTasks;
  }

  public int getNumOverdueTasks() {
    return numOverdueTasks;
  }

  public long getAvgTaskLagMillis() {
    return avgTaskLagMillis;
  }

  public int getNumActiveSlaves() {
    return numActiveSlaves;
  }

  public int getNumLostSlaves() {
    return numLostSlaves;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityDisasterStats that = (SingularityDisasterStats) o;
    return timestamp == that.timestamp &&
      numActiveTasks == that.numActiveTasks &&
      numPendingTasks == that.numPendingTasks &&
      numOverdueTasks == that.numOverdueTasks &&
      avgTaskLagMillis == that.avgTaskLagMillis &&
      numActiveSlaves == that.numActiveSlaves &&
      numLostSlaves == that.numLostSlaves;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(timestamp, numActiveTasks, numPendingTasks, numOverdueTasks, avgTaskLagMillis, numActiveSlaves, numLostSlaves);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("timestamp", timestamp)
      .add("numActiveTasks", numActiveTasks)
      .add("numPendingTasks", numPendingTasks)
      .add("numOverdueTasks", numOverdueTasks)
      .add("avgTaskLagMillis", avgTaskLagMillis)
      .add("numActiveSlaves", numActiveSlaves)
      .add("numLostSlaves", numLostSlaves)
      .toString();
  }
}
