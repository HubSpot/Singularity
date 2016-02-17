package com.hubspot.singularity;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Iterables;

public class SingularityTaskHistoryUpdate extends SingularityTaskIdHolder implements Comparable<SingularityTaskHistoryUpdate> {

  private final long timestamp;
  private final ExtendedTaskState taskState;
  private final Optional<String> statusMessage;
  private final Optional<String> statusReason;

  public enum SimplifiedTaskState {
    UNKNOWN, WAITING, RUNNING, DONE
  }

  public static Optional<SingularityTaskHistoryUpdate> getUpdate(final Iterable<SingularityTaskHistoryUpdate> updates, final ExtendedTaskState taskState) {
    return Iterables.tryFind(updates, new Predicate<SingularityTaskHistoryUpdate>() {
      @Override
      public boolean apply(@Nonnull SingularityTaskHistoryUpdate input) {
        return input.getTaskState() == taskState;
      }
    });
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

  @JsonCreator
  public SingularityTaskHistoryUpdate(@JsonProperty("taskId") SingularityTaskId taskId, @JsonProperty("timestamp") long timestamp, @JsonProperty("taskState") ExtendedTaskState taskState, @JsonProperty("statusMessage") Optional<String> statusMessage, @JsonProperty("statusReason") Optional<String> statusReason) {
    super(taskId);

    this.timestamp = timestamp;
    this.taskState = taskState;
    this.statusMessage = statusMessage;
    this.statusReason = statusReason;
  }

  @Override
  public int compareTo(SingularityTaskHistoryUpdate o) {
    return ComparisonChain.start()
        .compare(taskState.ordinal(), o.getTaskState().ordinal())
        .compare(timestamp, o.getTimestamp())
        .compare(o.getTaskId().getId(), getTaskId().getId())
        .result();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getTaskId(), timestamp, taskState, statusMessage, statusReason);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if ((other == null) || (other.getClass() != this.getClass())) {
      return false;
    }

    SingularityTaskHistoryUpdate that = (SingularityTaskHistoryUpdate) other;

    return Objects.equal(this.getTaskId(), that.getTaskId())
        && Objects.equal(this.timestamp, that.timestamp)
        && Objects.equal(this.taskState, that.taskState)
        && Objects.equal(this.statusMessage, that.statusMessage)
        && Objects.equal(this.statusReason, that.statusReason);
  }

  public long getTimestamp() {
    return timestamp;
  }

  public ExtendedTaskState getTaskState() {
    return taskState;
  }

  public Optional<String> getStatusMessage() {
    return statusMessage;
  }

  public Optional<String> getStatusReason() {
    return statusReason;
  }

  @Override public String toString() {
    return "SingularityTaskHistoryUpdate[" +
        "timestamp=" + timestamp +
        ", taskState=" + taskState +
        ", statusMessage=" + statusMessage +
        ", statusReason=" + statusReason +
        ']';
  }
}
