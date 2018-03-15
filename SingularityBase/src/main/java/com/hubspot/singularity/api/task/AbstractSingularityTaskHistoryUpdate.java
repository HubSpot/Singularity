package com.hubspot.singularity.api.task;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.immutables.value.Value.Immutable;

import com.google.common.collect.ComparisonChain;
import com.hubspot.singularity.annotations.SingularityStyle;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema(description = "Describes an update to the state of a task")
public abstract class AbstractSingularityTaskHistoryUpdate implements Comparable<SingularityTaskHistoryUpdate>, SingularityTaskIdHolder {
  public static Optional<SingularityTaskHistoryUpdate> getUpdate(final Collection<SingularityTaskHistoryUpdate> updates, final ExtendedTaskState taskState) {
    return updates.stream().filter((input) -> input.getTaskState() == taskState).findFirst();
  }

  public static SimplifiedTaskState getCurrentState(Iterable<SingularityTaskHistoryUpdate> updates) {
    SimplifiedTaskState state = SimplifiedTaskState.UNKNOWN;

    if (updates == null) {
      return state;
    }

    for (SingularityTaskHistoryUpdate update : updates) {
      if (update.getTaskState().isDone()) {
        return SimplifiedTaskState.DONE;
      } else if (update.getTaskState() == ExtendedTaskState.TASK_RUNNING) {
        state = SimplifiedTaskState.RUNNING;
      } else if (state == SimplifiedTaskState.UNKNOWN) {
        state = SimplifiedTaskState.WAITING;
      }
    }

    return state;
  }

  public SingularityTaskHistoryUpdate withPrevious(SingularityTaskHistoryUpdate previousUpdate) {
    Set<SingularityTaskHistoryUpdate> newPreviousUpdates = getFlattenedPreviousUpdates(this);
    newPreviousUpdates.add(previousUpdate.withoutPrevious());
    newPreviousUpdates.addAll(getFlattenedPreviousUpdates(previousUpdate));
    return SingularityTaskHistoryUpdate.copyOf(this).withPrevious(newPreviousUpdates);
  }

  private Set<SingularityTaskHistoryUpdate> getFlattenedPreviousUpdates(AbstractSingularityTaskHistoryUpdate update) {
    Set<SingularityTaskHistoryUpdate> previousUpdates = new HashSet<>();
    for (SingularityTaskHistoryUpdate previousUpdate : update.getPrevious()) {
      previousUpdates.add(previousUpdate.withoutPrevious());
      previousUpdates.addAll(getFlattenedPreviousUpdates(previousUpdate));
    }
    return previousUpdates;
  }

  protected SingularityTaskHistoryUpdate withoutPrevious() {
    return SingularityTaskHistoryUpdate.copyOf(this).withPrevious(Collections.emptyList());
  }

  @Override
  public int compareTo(SingularityTaskHistoryUpdate o) {
    return ComparisonChain.start()
        .compare(getTaskState().ordinal(), o.getTaskState().ordinal())
        .compare(getTimestamp(), o.getTimestamp())
        .compare(o.getTaskId().getId(), getTaskId().getId())
        .result();
  }

  @Schema(description = "Task id")
  public abstract SingularityTaskId getTaskId();

  @Schema(description = "The time at which this state update occurred")
  public abstract long getTimestamp();

  @Schema(description = "The new state of the task")
  public abstract ExtendedTaskState getTaskState();

  @Schema(description = "An optional message describing update", nullable = true)
  public abstract Optional<String> getStatusMessage();

  @Schema(description = "An optional message describing the reason for the update", nullable = true)
  public abstract Optional<String> getStatusReason();

  @Schema(description = "A list of task history updates of the same state. For example, multiple cleanups can be created for the same task")
  public abstract Set<SingularityTaskHistoryUpdate> getPrevious();
}
