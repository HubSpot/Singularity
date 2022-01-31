package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Describes the current scheduled tasks in Singularity")
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

  @Schema(description = "List of late task ids")
  public List<SingularityPendingTaskId> getLateTasks() {
    return lateTasks;
  }

  @Schema(description = "List of on demand late task ids")
  public List<SingularityPendingTaskId> getOnDemandLateTasks() {
    return onDemandLateTasks;
  }

  @Schema(description = "Number of future tasks")
  public int getNumFutureTasks() {
    return numFutureTasks;
  }

  @Schema(description = "Maximum task lag in ms")
  public long getMaxTaskLag() {
    return maxTaskLag;
  }

  @Schema(description = "Timestamp")
  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public String toString() {
    return (
      "SingularityScheduledTasksInfo{" +
      "lateTasks=" +
      lateTasks +
      ", onDemandLateTasks=" +
      onDemandLateTasks +
      ", numFutureTasks" +
      numFutureTasks +
      ", maxTaskLag" +
      maxTaskLag +
      ", timestamp" +
      timestamp +
      "}"
    );
  }
}
