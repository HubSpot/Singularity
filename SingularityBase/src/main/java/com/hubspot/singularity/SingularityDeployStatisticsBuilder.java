package com.hubspot.singularity;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SingularityDeployStatisticsBuilder {
  private final String requestId;
  private final String deployId;

  private int numTasks;

  private int numSuccess;
  private int numFailures;

  private int numSequentialRetries;

  // No longer used
  private ListMultimap<Integer, Long> instanceSequentialFailureTimestamps;

  private Optional<Long> lastFinishAt;
  private Optional<ExtendedTaskState> lastTaskState;

  private Optional<Long> averageRuntimeMillis;
  private Optional<Long> averageSchedulingDelayMillis; // Delta between between when each task was supposed to run vs. actually being submitted to Mesos

  private List<TaskFailureEvent> taskFailureEvents;

  public SingularityDeployStatisticsBuilder(String requestId, String deployId) {
    this.requestId = requestId;
    this.deployId = deployId;

    this.lastFinishAt = Optional.empty();
    this.lastTaskState = Optional.empty();
    this.averageRuntimeMillis = Optional.empty();
    this.averageSchedulingDelayMillis = Optional.empty();
  }

  public SingularityDeployStatistics build() {
    return new SingularityDeployStatistics(
      requestId,
      deployId,
      numSuccess,
      numFailures,
      numSequentialRetries,
      lastFinishAt,
      lastTaskState,
      null,
      numTasks,
      averageRuntimeMillis,
      averageSchedulingDelayMillis,
      taskFailureEvents
    );
  }

  @Deprecated
  public ListMultimap<Integer, Long> getInstanceSequentialFailureTimestamps() {
    return ImmutableListMultimap.of();
  }

  @Deprecated
  public SingularityDeployStatisticsBuilder setInstanceSequentialFailureTimestamps(
    ListMultimap<Integer, Long> instanceSequentialFailureTimestamps
  ) {
    this.instanceSequentialFailureTimestamps = instanceSequentialFailureTimestamps;
    return this;
  }

  public int getNumSuccess() {
    return numSuccess;
  }

  public SingularityDeployStatisticsBuilder setNumSuccess(int numSuccess) {
    this.numSuccess = numSuccess;
    return this;
  }

  public int getNumFailures() {
    return numFailures;
  }

  public SingularityDeployStatisticsBuilder setNumFailures(int numFailures) {
    this.numFailures = numFailures;
    return this;
  }

  public int getNumSequentialRetries() {
    return numSequentialRetries;
  }

  public SingularityDeployStatisticsBuilder setNumSequentialRetries(
    int numSequentialRetries
  ) {
    this.numSequentialRetries = numSequentialRetries;
    return this;
  }

  public Optional<Long> getLastFinishAt() {
    return lastFinishAt;
  }

  public SingularityDeployStatisticsBuilder setLastFinishAt(Optional<Long> lastFinishAt) {
    this.lastFinishAt = lastFinishAt;
    return this;
  }

  public Optional<ExtendedTaskState> getLastTaskState() {
    return lastTaskState;
  }

  public SingularityDeployStatisticsBuilder setLastTaskState(
    Optional<ExtendedTaskState> lastTaskState
  ) {
    this.lastTaskState = lastTaskState;
    return this;
  }

  public String getRequestId() {
    return requestId;
  }

  public int getNumTasks() {
    return numTasks;
  }

  public SingularityDeployStatisticsBuilder setNumTasks(int numTasks) {
    this.numTasks = numTasks;
    return this;
  }

  public Optional<Long> getAverageRuntimeMillis() {
    return averageRuntimeMillis;
  }

  public SingularityDeployStatisticsBuilder setAverageRuntimeMillis(
    Optional<Long> averageRuntimeMillis
  ) {
    this.averageRuntimeMillis = averageRuntimeMillis;
    return this;
  }

  public Optional<Long> getAverageSchedulingDelayMillis() {
    return averageSchedulingDelayMillis;
  }

  public SingularityDeployStatisticsBuilder setAverageSchedulingDelayMillis(
    Optional<Long> averageSchedulingDelayMillis
  ) {
    this.averageSchedulingDelayMillis = averageSchedulingDelayMillis;
    return this;
  }

  public String getDeployId() {
    return deployId;
  }

  public List<TaskFailureEvent> getTaskFailureEvents() {
    return taskFailureEvents;
  }

  public SingularityDeployStatisticsBuilder setTaskFailureEvents(
    List<TaskFailureEvent> taskFailureEvents
  ) {
    this.taskFailureEvents = taskFailureEvents;
    return this;
  }

  public SingularityDeployStatisticsBuilder addTaskFailureEvent(
    TaskFailureEvent taskFailureEvent
  ) {
    taskFailureEvents.add(taskFailureEvent);
    return this;
  }

  public SingularityDeployStatisticsBuilder trimTaskFailureEvents(int countToKeep) {
    this.taskFailureEvents =
      taskFailureEvents
        .stream()
        .sorted(Comparator.comparingLong(TaskFailureEvent::getTimestamp).reversed())
        .limit(countToKeep)
        .collect(Collectors.toList());
    return this;
  }

  @Override
  public String toString() {
    return (
      "SingularityDeployStatisticsBuilder{" +
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
      ", instanceSequentialFailureTimestamps=" +
      instanceSequentialFailureTimestamps +
      ", lastFinishAt=" +
      lastFinishAt +
      ", lastTaskState=" +
      lastTaskState +
      ", averageRuntimeMillis=" +
      averageRuntimeMillis +
      '}'
    );
  }
}
