package com.hubspot.singularity.api.task;

import java.util.List;
import java.util.Optional;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hubspot.singularity.annotations.SingularityStyle;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema(description = "Describes the current state of a task")
public abstract class AbstractSingularityTaskState {
  @Schema(
      title = "The unique id for this task",
      nullable = true,
      description = "Will be present if `pending` is `false` (i.e. the task has been assigned an id and launched)"
  )
  public abstract Optional<SingularityTaskId> getTaskId();

  @Schema(
      title = "A unique id describing a task that is waiting to launch",
      nullable = true
  )
  public abstract Optional<SingularityPendingTaskId> getPendingTaskId();

  @Schema(
      title = "The run id associated with this task if one is present",
      nullable = true
  )
  public abstract Optional<String> getRunId();

  @Schema(
      title = "The current state of this task",
      description = "Present if the task has already been launched",
      nullable = true
  )
  public abstract Optional<ExtendedTaskState> getCurrentState();

  @Schema(
      title = "A list of state updates for this task",
      description = "Empty if the task has not yet been launched"
  )
  public abstract List<SingularityTaskHistoryUpdate> getTaskHistory();

  @Schema(description = "true if the task is still waiting to be launched")
  public abstract boolean isPending();

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

  @JsonIgnore
  public boolean isFailed() {
    return getCurrentState().isPresent() && getCurrentState().get().isFailed();
  }

  @JsonIgnore
  public boolean isDone() {
    return getCurrentState().isPresent() && getCurrentState().get().isDone();
  }

  @JsonIgnore
  public boolean isSuccess() {
    return getCurrentState().isPresent() && getCurrentState().get().isSuccess();
  }
}
