package com.hubspot.singularity.api.task;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ComparisonChain;
import com.hubspot.singularity.annotations.SingularityStyle;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema(description = "A task id and latest state for a task")
public abstract class AbstractSingularityTaskIdHistory implements Comparable<SingularityTaskIdHistory> {

  public static SingularityTaskIdHistory fromTaskIdAndTaskAndUpdates(SingularityTaskId taskId, SingularityTask task, List<SingularityTaskHistoryUpdate> updates) {
    ExtendedTaskState lastTaskState = null;
    long updatedAt = taskId.getStartedAt();

    if (updates != null && !updates.isEmpty()) {
      SingularityTaskHistoryUpdate lastUpdate = Collections.max(updates);
      lastTaskState = lastUpdate.getTaskState();
      updatedAt = lastUpdate.getTimestamp();
    }

    return new SingularityTaskIdHistory(taskId, updatedAt, Optional.ofNullable(lastTaskState), task.getTaskRequest().getPendingTask().getRunId());
  }

  @Override
  public int compareTo(SingularityTaskIdHistory o) {
    return ComparisonChain.start()
      .compare(o.getUpdatedAt(), getUpdatedAt())
      .compare(getTaskId().getId(), o.getTaskId().getId())
      .result();
  }

  @Schema(description = "Task id")
  public abstract SingularityTaskId getTaskId();

  @Schema(description = "The timestamp of the latest update for the task")
  public abstract long getUpdatedAt();

  @JsonProperty("lastStatus")
  @Schema(description = "The latest state of the task", nullable = true)
  public abstract Optional<ExtendedTaskState> getLastTaskState();

  @Schema(description = "A runId associated with this task", nullable = true)
  public abstract Optional<String> getRunId();
}
