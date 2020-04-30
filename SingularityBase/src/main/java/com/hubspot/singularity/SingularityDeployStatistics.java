package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Schema(description = "Task statistics related to a particular deploy")
public class SingularityDeployStatistics {
  private final String requestId;
  private final String deployId;
  private final int numTasks;
  private final int numSuccess;
  private final int numFailures;
  private final int numSequentialRetries;
  private final Optional<Long> lastFinishAt;
  private final Optional<ExtendedTaskState> lastTaskState;
  private final Optional<Long> averageRuntimeMillis;
  private final Optional<Long> averageSchedulingDelayMillis;
  private final List<TaskFailureEvent> taskFailureEvents;

  @JsonCreator
  public SingularityDeployStatistics(
    @JsonProperty("requestId") String requestId,
    @JsonProperty("deployId") String deployId,
    @JsonProperty("numSuccess") int numSuccess,
    @JsonProperty("numFailures") int numFailures,
    @JsonProperty("numSequentialRetries") int numSequentialRetries,
    @JsonProperty("lastFinishAt") Optional<Long> lastFinishAt,
    @JsonProperty("lastTaskState") Optional<ExtendedTaskState> lastTaskState,
    @JsonProperty(
      "instanceSequentialFailureTimestamps"
    ) ListMultimap<Integer, Long> instanceSequentialFailureTimestamps,
    @JsonProperty("numTasks") int numTasks,
    @JsonProperty("averageRuntimeMillis") Optional<Long> averageRuntimeMillis,
    @JsonProperty(
      "averageSchedulingDelayMillis"
    ) Optional<Long> averageSchedulingDelayMillis,
    @JsonProperty("taskFailureEvents") List<TaskFailureEvent> taskFailureEvents
  ) {
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
    this.taskFailureEvents =
      taskFailureEvents == null ? Collections.emptyList() : taskFailureEvents;
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
      .setTaskFailureEvents(new ArrayList<>(taskFailureEvents));
  }

  @Schema(description = "The number of tasks associated with this deploy")
  public int getNumTasks() {
    return numTasks;
  }

  @Schema(
    description = "Average runtime of tasks associated with this deploy",
    nullable = true
  )
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

  @Schema(
    description = "Number of sequential successful tasks (used in cooldown calculations)"
  )
  public int getNumSuccess() {
    return numSuccess;
  }

  @Schema(
    description = "Number of sequential failed tasks (used in cooldown calculations)"
  )
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

  @Schema(
    description = "Number of retries for tasks in this deploy, relevant for scheduled request types"
  )
  public int getNumSequentialRetries() {
    return numSequentialRetries;
  }

  @Schema(description = "Timestamps of failed tasks by instance number")
  @Deprecated
  public ListMultimap<Integer, Long> getInstanceSequentialFailureTimestamps() {
    return ImmutableListMultimap.of();
  }

  @Schema(description = "Timestamps and descriptions of failed tasks by instance number")
  public List<TaskFailureEvent> getTaskFailureEvents() {
    return taskFailureEvents;
  }

  @Override
  public String toString() {
    return (
      "SingularityDeployStatistics{" +
      "requestId='" +
      requestId +
      '\'' +
      ", deployId='" +
      deployId +
      '\'' +
      ", numTasks=" +
      numTasks +
      ", numSuccess=" +
      numSuccess +
      ", numFailures=" +
      numFailures +
      ", numSequentialRetries=" +
      numSequentialRetries +
      ", lastFinishAt=" +
      lastFinishAt +
      ", lastTaskState=" +
      lastTaskState +
      ", averageRuntimeMillis=" +
      averageRuntimeMillis +
      ", averageSchedulingDelayMillis=" +
      averageSchedulingDelayMillis +
      '}'
    );
  }
}
