package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Schema(description = "Describes the current state of a task")
public class SingularityTaskState {
  private final Optional<SingularityTaskId> taskId;
  private final Optional<SingularityPendingTaskId> pendingTaskId;
  private final Optional<String> runId;
  private final Optional<ExtendedTaskState> currentState;
  private final List<SingularityTaskHistoryUpdate> taskHistory;
  private final boolean pending;

  @JsonCreator
  public SingularityTaskState(
    @JsonProperty("taskId") Optional<SingularityTaskId> taskId,
    @JsonProperty("pendingTaskId") Optional<SingularityPendingTaskId> pendingTaskId,
    @JsonProperty("runId") Optional<String> runId,
    @JsonProperty("currentState") Optional<ExtendedTaskState> currentState,
    @JsonProperty("taskHistory") List<SingularityTaskHistoryUpdate> taskHistory,
    @JsonProperty("pending") boolean pending
  ) {
    this.taskId = taskId;
    this.pendingTaskId = pendingTaskId;
    this.runId = runId;
    this.currentState = currentState;
    this.taskHistory = taskHistory != null ? taskHistory : Collections.emptyList();
    this.pending = pending;
  }

  @Deprecated
  public SingularityTaskState(
    Optional<SingularityTaskId> taskId,
    SingularityPendingTaskId pendingTaskId,
    Optional<String> runId,
    Optional<ExtendedTaskState> currentState,
    List<SingularityTaskHistoryUpdate> taskHistory,
    boolean pending
  ) {
    this(taskId, Optional.of(pendingTaskId), runId, currentState, taskHistory, pending);
  }

  public static SingularityTaskState fromTaskHistory(SingularityTaskHistory taskHistory) {
    return new SingularityTaskState(
      Optional.of(taskHistory.getTask().getTaskId()),
      Optional.of(
        taskHistory.getTask().getTaskRequest().getPendingTask().getPendingTaskId()
      ),
      taskHistory.getTask().getTaskRequest().getPendingTask().getRunId(),
      Optional.of(taskHistory.getLastTaskUpdate().get().getTaskState()),
      taskHistory.getTaskUpdates(),
      false
    );
  }

  @Schema(
    title = "The unique id for this task",
    nullable = true,
    description = "Will be present if `pending` is `false` (i.e. the task has been assigned an id and launched)"
  )
  public Optional<SingularityTaskId> getTaskId() {
    return taskId;
  }

  @Schema(
    title = "A unique id describing a task that is waiting to launch",
    nullable = true
  )
  public SingularityPendingTaskId getPendingTaskId() {
    return pendingTaskId.orElse(null);
  }

  @JsonIgnore
  public Optional<SingularityPendingTaskId> getMaybePendingTaskId() {
    return pendingTaskId;
  }

  @Schema(
    title = "The run id associated with this task if one is present",
    nullable = true
  )
  public Optional<String> getRunId() {
    return runId;
  }

  @Schema(
    title = "The current state of this task",
    description = "Present if the task has already been launched",
    nullable = true
  )
  public Optional<ExtendedTaskState> getCurrentState() {
    return currentState;
  }

  @Schema(
    title = "A list of state updates for this task",
    description = "Empty if the task has not yet been launched"
  )
  public List<SingularityTaskHistoryUpdate> getTaskHistory() {
    return taskHistory;
  }

  @Schema(description = "true if the task is still waiting to be launched")
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
    if (
      pendingTaskId != null
        ? !pendingTaskId.equals(that.pendingTaskId)
        : that.pendingTaskId != null
    ) {
      return false;
    }
    if (runId != null ? !runId.equals(that.runId) : that.runId != null) {
      return false;
    }
    if (
      currentState != null
        ? !currentState.equals(that.currentState)
        : that.currentState != null
    ) {
      return false;
    }
    return taskHistory != null
      ? taskHistory.equals(that.taskHistory)
      : that.taskHistory == null;
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
    return (
      "SingularityTaskState{" +
      "taskId=" +
      taskId +
      ", pendingTaskId=" +
      pendingTaskId +
      ", runId=" +
      runId +
      ", currentState=" +
      currentState +
      ", taskHistory=" +
      taskHistory +
      ", pending=" +
      pending +
      '}'
    );
  }
}
