package com.hubspot.singularity;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityTaskState {
  private final SingularityId id;
  private final Optional<String> runId;
  private final Optional<ExtendedTaskState> currentState;
  private final List<SingularityTaskHistoryUpdate> taskHistory;
  private final boolean pending;

  @JsonCreator
  public SingularityTaskState(@JsonProperty("taskId") SingularityId id,
                              @JsonProperty("runId") Optional<String> runId,
                              @JsonProperty("currentState") Optional<ExtendedTaskState> currentState,
                              @JsonProperty("taskHistory") List<SingularityTaskHistoryUpdate> taskHistory,
                              @JsonProperty("pending") boolean pending) {
    this.id = id;
    this.runId = runId;
    this.currentState = currentState;
    this.taskHistory = taskHistory != null ? taskHistory : Collections.emptyList();
    this.pending = pending;
  }

  public static SingularityTaskState fromTaskHistory(SingularityTaskHistory taskHistory) {
    return new SingularityTaskState(
        taskHistory.getTask().getTaskId(),
        taskHistory.getTask().getTaskRequest().getPendingTask().getRunId(),
        Optional.of(taskHistory.getLastTaskUpdate().get().getTaskState()),
        taskHistory.getTaskUpdates(),
        false
    );
  }

  /*
   * If the task is pending, id will be a SingularityPendingTaskId
   * otherwise id will be a SingularityTaskId
   */
  public SingularityId getId() {
    return id;
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
    if (id != null ? !id.equals(that.id) : that.id != null) {
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
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (runId != null ? runId.hashCode() : 0);
    result = 31 * result + (currentState != null ? currentState.hashCode() : 0);
    result = 31 * result + (taskHistory != null ? taskHistory.hashCode() : 0);
    result = 31 * result + (pending ? 1 : 0);
    return result;
  }

  @Override
  public String toString() {
    return "SingularityTaskState{" +
        "id=" + id +
        ", runId=" + runId +
        ", currentState=" + currentState +
        ", taskHistory=" + taskHistory +
        ", pending=" + pending +
        '}';
  }
}
