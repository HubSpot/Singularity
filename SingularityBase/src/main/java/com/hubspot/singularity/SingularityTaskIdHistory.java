package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ComparisonChain;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Schema(description = "A task id and latest state for a task")
public class SingularityTaskIdHistory implements Comparable<SingularityTaskIdHistory> {

  private final SingularityTaskId taskId;
  private final long updatedAt;
  private final Optional<ExtendedTaskState> lastTaskState;
  private final Optional<String> runId;

  public static SingularityTaskIdHistory fromTaskIdAndTaskAndUpdates(
    SingularityTaskId taskId,
    SingularityTask task,
    List<SingularityTaskHistoryUpdate> updates
  ) {
    ExtendedTaskState lastTaskState = null;
    long updatedAt = taskId.getStartedAt();

    if (updates != null) {
      Optional<SingularityTaskHistoryUpdate> maybeLastUpdate = updates
        .stream()
        .filter(Objects::nonNull)
        .max(SingularityTaskHistoryUpdate::compareTo);
      if (maybeLastUpdate.isPresent()) {
        lastTaskState = maybeLastUpdate.get().getTaskState();
        updatedAt = maybeLastUpdate.get().getTimestamp();
      }
    }

    return new SingularityTaskIdHistory(
      taskId,
      updatedAt,
      Optional.ofNullable(lastTaskState),
      task.getTaskRequest().getPendingTask().getRunId()
    );
  }

  @JsonCreator
  public SingularityTaskIdHistory(
    @JsonProperty("taskId") SingularityTaskId taskId,
    @JsonProperty("updatedAt") long updatedAt,
    @JsonProperty("lastStatus") Optional<ExtendedTaskState> lastTaskState,
    @JsonProperty("runId") Optional<String> runId
  ) {
    this.taskId = taskId;
    this.updatedAt = updatedAt;
    this.lastTaskState = lastTaskState;
    this.runId = runId;
  }

  @Override
  public int compareTo(SingularityTaskIdHistory o) {
    return ComparisonChain
      .start()
      .compare(o.getUpdatedAt(), updatedAt)
      .compare(taskId.getId(), o.getTaskId().getId())
      .result();
  }

  @Schema(description = "Task id")
  public SingularityTaskId getTaskId() {
    return taskId;
  }

  @Schema(description = "The latest state of the task", nullable = true)
  public Optional<ExtendedTaskState> getLastTaskState() {
    return lastTaskState;
  }

  @Schema(description = "The timestamp of the latest update for the task")
  public long getUpdatedAt() {
    return updatedAt;
  }

  @Schema(description = "A runId associated with this task", nullable = true)
  public Optional<String> getRunId() {
    return runId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityTaskIdHistory that = (SingularityTaskIdHistory) o;
    return (
      updatedAt == that.updatedAt &&
      Objects.equals(taskId, that.taskId) &&
      Objects.equals(lastTaskState, that.lastTaskState) &&
      Objects.equals(runId, that.runId)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(taskId, updatedAt, lastTaskState, runId);
  }

  @Override
  public String toString() {
    return (
      "SingularityTaskIdHistory{" +
      "taskId=" +
      taskId +
      ", updatedAt=" +
      updatedAt +
      ", lastTaskState=" +
      lastTaskState +
      ", runId=" +
      runId +
      '}'
    );
  }
}
