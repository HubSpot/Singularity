package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Task statistics related to a particular deploy")
public class SingularityDeployStatistics {

  private final String requestId;
  private final String deployId;
  private final int numTasks;
  private final int numSuccess;
  private final int numFailures;
  private final int numSequentialRetries;
  private final ListMultimap<Integer, Long> instanceSequentialFailureTimestamps;
  private final Optional<Long> lastFinishAt;
  private final Optional<ExtendedTaskState> lastTaskState;
  private final Optional<Long> averageRuntimeMillis;
  private final Optional<Long> averageSchedulingDelayMillis;

  @JsonCreator
  public SingularityDeployStatistics(@JsonProperty("requestId") String requestId,
                                     @JsonProperty("deployId") String deployId,
                                     @JsonProperty("numSuccess") int numSuccess,
                                     @JsonProperty("numFailures") int numFailures,
                                     @JsonProperty("numSequentialRetries") int numSequentialRetries,
                                     @JsonProperty("lastFinishAt") Optional<Long> lastFinishAt,
                                     @JsonProperty("lastTaskState") Optional<ExtendedTaskState> lastTaskState,
                                     @JsonProperty("instanceSequentialFailureTimestamps") ListMultimap<Integer, Long> instanceSequentialFailureTimestamps,
                                     @JsonProperty("numTasks") int numTasks,
                                     @JsonProperty("averageRuntimeMillis") Optional<Long> averageRuntimeMillis,
                                     @JsonProperty("averageSchedulingDelayMillis") Optional<Long> averageSchedulingDelayMillis) {
    this.requestId = requestId;
    this.deployId = deployId;
    this.numSuccess = numSuccess;
    this.numFailures = numFailures;
    this.lastFinishAt = lastFinishAt;
    this.lastTaskState = lastTaskState;
    this.numSequentialRetries = numSequentialRetries;
    this.numTasks = numTasks;
    this.averageRuntimeMillis = averageRuntimeMillis;
    this.averageSchedulingDelayMillis = averageSchedulingDelayMillis;
    this.instanceSequentialFailureTimestamps = instanceSequentialFailureTimestamps == null ?  ImmutableListMultimap.<Integer, Long> of() : ImmutableListMultimap.copyOf(instanceSequentialFailureTimestamps);
  }

  public SingularityDeployStatisticsBuilder toBuilder() {
    return new SingularityDeployStatisticsBuilder(requestId, deployId)
    .setLastFinishAt(lastFinishAt)
    .setLastTaskState(lastTaskState)
    .setNumSequentialRetries(numSequentialRetries)
    .setNumFailures(numFailures)
    .setNumSuccess(numSuccess)
    .setNumTasks(numTasks)
    .setAverageRuntimeMillis(averageRuntimeMillis)
    .setAverageSchedulingDelayMillis(averageSchedulingDelayMillis)
    .setInstanceSequentialFailureTimestamps(ArrayListMultimap.create(instanceSequentialFailureTimestamps));
  }

  @Schema(description = "The number of tasks associated with this deploy")
  public int getNumTasks() {
    return numTasks;
  }

  @Schema(description = "Average runtime of tasks associated with this deploy", nullable = true)
  public Optional<Long> getAverageRuntimeMillis() {
    return averageRuntimeMillis;
  }

  @Schema(description = "Average delay launching tasks for this deploy", nullable = true)
  public Optional<Long> getAverageSchedulingDelayMillis() {
    return averageSchedulingDelayMillis;
  }

  @Schema(description = "Request id")
  public String getRequestId() {
    return requestId;
  }

  @Schema(description = "Deploy id")
  public String getDeployId() {
    return deployId;
  }

  @Schema(description = "Number of tasks that have finished successfully for this deploy")
  public int getNumSuccess() {
    return numSuccess;
  }

  @Schema(description = "Number of tasks that have finished with a failure for this deploy")
  public int getNumFailures() {
    return numFailures;
  }

  @Schema(description = "Time of the last finished task for this deploy", nullable = true)
  public Optional<Long> getLastFinishAt() {
    return lastFinishAt;
  }

  @Schema(description = "The most recent task state for this deploy", nullable = true)
  public Optional<ExtendedTaskState> getLastTaskState() {
    return lastTaskState;
  }

  @Schema(description = "Number of retries for tasks in this deploy, relevant for scheduled request types")
  public int getNumSequentialRetries() {
    return numSequentialRetries;
  }

  @Schema(description = "Timestamps of failed tasks by instance number")
  public ListMultimap<Integer, Long> getInstanceSequentialFailureTimestamps() {
    return instanceSequentialFailureTimestamps;
  }

  @Override
  public String toString() {
    return "SingularityDeployStatistics{" +
        "requestId='" + requestId + '\'' +
        ", deployId='" + deployId + '\'' +
        ", numTasks=" + numTasks +
        ", numSuccess=" + numSuccess +
        ", numFailures=" + numFailures +
        ", numSequentialRetries=" + numSequentialRetries +
        ", instanceSequentialFailureTimestamps=" + instanceSequentialFailureTimestamps +
        ", lastFinishAt=" + lastFinishAt +
        ", lastTaskState=" + lastTaskState +
        ", averageRuntimeMillis=" + averageRuntimeMillis +
        ", averageSchedulingDelayMillis=" + averageSchedulingDelayMillis +
        '}';
  }
}
