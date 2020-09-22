package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.primitives.Longs;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "Data collected to identify disasters")
public class SingularityDisasterDataPoint
  implements Comparable<SingularityDisasterDataPoint> {
  private final long timestamp;
  private final int numActiveTasks;
  private final int numPendingTasks;
  private final int numLateTasks;
  private final long avgTaskLagMillis;
  private final int numLostTasks;
  private final int numActiveAgents;
  private final int numLostAgents;

  public SingularityDisasterDataPoint(
    long timestamp,
    int numActiveTasks,
    int numPendingTasks,
    int numLateTasks,
    long avgTaskLagMillis,
    int numLostTasks,
    int numActiveAgents,
    int numLostAgents
  ) {
    this(
      timestamp,
      numActiveTasks,
      numPendingTasks,
      numLateTasks,
      avgTaskLagMillis,
      numLostTasks,
      numActiveAgents,
      numLostAgents,
      null,
      null
    );
  }

  @JsonCreator
  public SingularityDisasterDataPoint(
    @JsonProperty("timestamp") long timestamp,
    @JsonProperty("numActiveTasks") int numActiveTasks,
    @JsonProperty("numPendingTasks") int numPendingTasks,
    @JsonProperty("numLateTasks") int numLateTasks,
    @JsonProperty("avgTaskLagMillis") long avgTaskLagMillis,
    @JsonProperty("numLostTasks") int numLostTasks,
    @JsonProperty("numActiveAgents") Integer numActiveAgents,
    @JsonProperty("numLostAgents") Integer numLostAgents,
    @JsonProperty("numActiveSlaves") Integer numActiveSlaves,
    @JsonProperty("numLostSlaves") Integer numLostSlaves
  ) {
    this.timestamp = timestamp;
    this.numActiveTasks = numActiveTasks;
    this.numPendingTasks = numPendingTasks;
    this.numLateTasks = numLateTasks;
    this.avgTaskLagMillis = avgTaskLagMillis;
    this.numLostTasks = numLostTasks;
    this.numActiveAgents = MoreObjects.firstNonNull(numActiveAgents, numActiveSlaves);
    this.numLostAgents = MoreObjects.firstNonNull(numLostAgents, numLostSlaves);
  }

  @Schema(description = "The time this data was collected")
  public long getTimestamp() {
    return timestamp;
  }

  @Schema(description = "A count of active tasks")
  public int getNumActiveTasks() {
    return numActiveTasks;
  }

  @Schema(description = "A count of pending tasks")
  public int getNumPendingTasks() {
    return numPendingTasks;
  }

  @Schema(description = "A count of late tasks")
  public int getNumLateTasks() {
    return numLateTasks;
  }

  @Schema(description = "The average task lag for all pending tasks")
  public long getAvgTaskLagMillis() {
    return avgTaskLagMillis;
  }

  @Schema(description = "A count of lost tasks")
  public int getNumLostTasks() {
    return numLostTasks;
  }

  @Schema(description = "A count of active agents")
  public int getNumActiveAgents() {
    return numActiveAgents;
  }

  @Schema(description = "A count of agents lost since the last data point was collected")
  public int getNumLostAgents() {
    return numLostAgents;
  }

  @Deprecated
  @Schema(description = "A count of active agents")
  public int getNumActiveSlaves() {
    return numActiveAgents;
  }

  @Deprecated
  @Schema(description = "A count of agents lost since the last data point was collected")
  public int getNumLostSlaves() {
    return numLostAgents;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityDisasterDataPoint that = (SingularityDisasterDataPoint) o;
    return (
      timestamp == that.timestamp &&
      numActiveTasks == that.numActiveTasks &&
      numPendingTasks == that.numPendingTasks &&
      numLateTasks == that.numLateTasks &&
      avgTaskLagMillis == that.avgTaskLagMillis &&
      numLostTasks == that.numLostTasks &&
      numActiveAgents == that.numActiveAgents &&
      numLostAgents == that.numLostAgents
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      timestamp,
      numActiveTasks,
      numPendingTasks,
      numLateTasks,
      avgTaskLagMillis,
      numLostTasks,
      numActiveAgents,
      numLostAgents
    );
  }

  @Override
  public String toString() {
    return (
      "SingularityDisasterDataPoint{" +
      "timestamp=" +
      timestamp +
      ", numActiveTasks=" +
      numActiveTasks +
      ", numPendingTasks=" +
      numPendingTasks +
      ", numLateTasks=" +
      numLateTasks +
      ", avgTaskLagMillis=" +
      avgTaskLagMillis +
      ", numLostTasks=" +
      numLostTasks +
      ", numActiveAgents=" +
      numActiveAgents +
      ", numLostAgents=" +
      numLostAgents +
      '}'
    );
  }

  @Override
  public int compareTo(SingularityDisasterDataPoint o) {
    return Longs.compare(o.getTimestamp(), this.timestamp);
  }
}
