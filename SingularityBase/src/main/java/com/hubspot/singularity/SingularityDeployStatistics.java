package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;

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
  public SingularityDeployStatistics(@JsonProperty("requestId") String requestId, @JsonProperty("deployId") String deployId, @JsonProperty("numSuccess") int numSuccess, @JsonProperty("numFailures") int numFailures,
      @JsonProperty("numSequentialRetries") int numSequentialRetries, @JsonProperty("lastFinishAt") Optional<Long> lastFinishAt, @JsonProperty("lastTaskState") Optional<ExtendedTaskState> lastTaskState,
      @JsonProperty("instanceSequentialFailureTimestamps") ListMultimap<Integer, Long> instanceSequentialFailureTimestamps, @JsonProperty("numTasks") int numTasks,
      @JsonProperty("averageRuntimeMillis") Optional<Long> averageRuntimeMillis, @JsonProperty("averageSchedulingDelayMillis") Optional<Long> averageSchedulingDelayMillis) {
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

  public int getNumTasks() {
    return numTasks;
  }

  public Optional<Long> getAverageRuntimeMillis() {
    return averageRuntimeMillis;
  }

  public Optional<Long> getAverageSchedulingDelayMillis() {
    return averageSchedulingDelayMillis;
  }

  public String getRequestId() {
    return requestId;
  }

  public String getDeployId() {
    return deployId;
  }

  public int getNumSuccess() {
    return numSuccess;
  }

  public int getNumFailures() {
    return numFailures;
  }

  public Optional<Long> getLastFinishAt() {
    return lastFinishAt;
  }

  public Optional<ExtendedTaskState> getLastTaskState() {
    return lastTaskState;
  }

  public int getNumSequentialRetries() {
    return numSequentialRetries;
  }

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
