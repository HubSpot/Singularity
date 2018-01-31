package com.hubspot.singularity;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityTaskState {
  private final Optional<SingularityTaskId> taskId;
  private final Optional<SingularityPendingTaskId> pendingTaskId;
  private final Optional<String> runId;
  private final Optional<ExtendedTaskState> currentState;
  private final List<SingularityTaskHistoryUpdate> taskHistory;
  private final boolean pending;

  @JsonCreator
  public SingularityTaskState(@JsonProperty("taskId") Optional<SingularityTaskId> taskId,
                              @JsonProperty("pendingTaskId") Optional<SingularityPendingTaskId> pendingTaskId,
                              @JsonProperty("runId") Optional<String> runId,
                              @JsonProperty("currentState") Optional<ExtendedTaskState> currentState,
                              @JsonProperty("taskHistory") List<SingularityTaskHistoryUpdate> taskHistory,
                              @JsonProperty("pending") boolean pending) {
    this.taskId = taskId;
    this.pendingTaskId = pendingTaskId;
    this.runId = runId;
    this.currentState = currentState;
    this.taskHistory = taskHistory != null ? taskHistory : Collections.emptyList();
    this.pending = pending;
  }

  @Deprecated
  public SingularityTaskState(Optional<SingularityTaskId> taskId, SingularityPendingTaskId pendingTaskId, Optional<String> runId, Optional<ExtendedTaskState> currentState, List<SingularityTaskHistoryUpdate> taskHistory, boolean pending) {
   this(taskId, Optional.of(pendingTaskId), runId, currentState, taskHistory, pending);
  }

  public static SingularityTaskState fromTaskHistory(SingularityTaskHistory taskHistory) {
    return new SingularityTaskState(
        Optional.of(taskHistory.getTask().getTaskId()),
        Optional.of(taskHistory.getTask().getTaskRequest().getPendingTask().getPendingTaskId()),
        taskHistory.getTask().getTaskRequest().getPendingTask().getRunId(),
        Optional.of(taskHistory.getLastTaskUpdate().get().getTaskState()),
        taskHistory.getTaskUpdates(),
        false
    );
  }

  /*
   * Will be present when pending is `false`
   */
  public Optional<SingularityTaskId> getTaskId() {
    return taskId;
  }

  public SingularityPendingTaskId getPendingTaskId() {
    return pendingTaskId.orNull();
  }

  @JsonIgnore
  public Optional<SingularityPendingTaskId> getMaybePendingTaskId() {
    return pendingTaskId;
  }

  public Optional<String> getRunId() {
    return runId;
  }

  public Optional<ExtendedTaskState> getCurrentState() {
    return currentState;
  }

  public List<SingularityTaskHistoryUpdate> getTaskHistory() {
    return taskHistory;
  }

  public boolean isPending() {
    return pending;
  }

  @JsonIgnore
  public boolean isFailed() {
    return currentState.isPresent() && currentState.get().isFailed();
  }

  @JsonIgnore
  public boolean isDone() {
    return currentState.isPresent() && currentState.get().isDone();
  }

  @JsonIgnore
  public boolean isSuccess() {
    return currentState.isPresent() && currentState.get().isSuccess();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SingularityTaskState that = (SingularityTaskState) o;

    if (pending != that.pending) {
      return false;
    }
    if (taskId != null ? !taskId.equals(that.taskId) : that.taskId != null) {
      return false;
    }
    if (pendingTaskId != null ? !pendingTaskId.equals(that.pendingTaskId) : that.pendingTaskId != null) {
      return false;
    }
    if (runId != null ? !runId.equals(that.runId) : that.runId != null) {
      return false;
    }
    if (currentState != null ? !currentState.equals(that.currentState) : that.currentState != null) {
      return false;
    }
    return taskHistory != null ? taskHistory.equals(that.taskHistory) : that.taskHistory == null;
  }

  @Override
  public int hashCode() {
    int result = taskId != null ? taskId.hashCode() : 0;
    result = 31 * result + (pendingTaskId != null ? pendingTaskId.hashCode() : 0);
    result = 31 * result + (runId != null ? runId.hashCode() : 0);
    result = 31 * result + (currentState != null ? currentState.hashCode() : 0);
    result = 31 * result + (taskHistory != null ? taskHistory.hashCode() : 0);
    result = 31 * result + (pending ? 1 : 0);
    return result;
  }

  @Override
  public String toString() {
    return "SingularityTaskState{" +
        "taskId=" + taskId +
        ", pendingTaskId=" + pendingTaskId +
        ", runId=" + runId +
        ", currentState=" + currentState +
        ", taskHistory=" + taskHistory +
        ", pending=" + pending +
        '}';
  }
}
