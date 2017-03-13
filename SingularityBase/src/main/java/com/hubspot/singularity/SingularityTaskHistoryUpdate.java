package com.hubspot.singularity;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Iterables;

public class SingularityTaskHistoryUpdate extends SingularityTaskIdHolder implements Comparable<SingularityTaskHistoryUpdate> {

  private final long timestamp;
  private final ExtendedTaskState taskState;
  private final Optional<String> statusMessage;
  private final Optional<String> statusReason;
  private final Set<SingularityTaskHistoryUpdate> previous;

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

  public SingularityTaskHistoryUpdate(SingularityTaskId taskId, long timestamp, ExtendedTaskState taskState, Optional<String> statusMessage, Optional<String> statusReason) {
    this(taskId, timestamp, taskState, statusMessage, statusReason, Collections.<SingularityTaskHistoryUpdate>emptySet());
  }

  @JsonCreator
  public SingularityTaskHistoryUpdate(@JsonProperty("taskId") SingularityTaskId taskId,
                                      @JsonProperty("timestamp") long timestamp,
                                      @JsonProperty("taskState") ExtendedTaskState taskState,
                                      @JsonProperty("statusMessage") Optional<String> statusMessage,
                                      @JsonProperty("statusReason") Optional<String> statusReason,
                                      @JsonProperty("previous") Set<SingularityTaskHistoryUpdate> previous) {
    super(taskId);

    this.timestamp = timestamp;
    this.taskState = taskState;
    this.statusMessage = statusMessage;
    this.statusReason = statusReason;
    this.previous = previous != null ? previous : Collections.<SingularityTaskHistoryUpdate>emptySet();
  }

  public SingularityTaskHistoryUpdate withPrevious(SingularityTaskHistoryUpdate previousUpdate) {
    Set<SingularityTaskHistoryUpdate> newPreviousUpdates = getFlattenedPreviousUpdates(this);
    newPreviousUpdates.add(previousUpdate.withoutPrevious());
    newPreviousUpdates.addAll(getFlattenedPreviousUpdates(previousUpdate));
    return new SingularityTaskHistoryUpdate(getTaskId(), timestamp, taskState, statusMessage, statusReason, newPreviousUpdates);
  }

  private Set<SingularityTaskHistoryUpdate> getFlattenedPreviousUpdates(SingularityTaskHistoryUpdate update) {
    Set<SingularityTaskHistoryUpdate> previousUpdates = new HashSet<>();
    for (SingularityTaskHistoryUpdate previousUpdate : update.getPrevious()) {
      previousUpdates.add(previousUpdate.withoutPrevious());
      previousUpdates.addAll(getFlattenedPreviousUpdates(previousUpdate));
    }
    return previousUpdates;
  }

  public SingularityTaskHistoryUpdate withoutPrevious() {
    return new SingularityTaskHistoryUpdate(getTaskId(), timestamp, taskState, statusMessage, statusReason, Collections.<SingularityTaskHistoryUpdate>emptySet());
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
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityTaskHistoryUpdate that = (SingularityTaskHistoryUpdate) o;
    return timestamp == that.timestamp &&
        taskState == that.taskState &&
        Objects.equals(statusMessage, that.statusMessage) &&
        Objects.equals(statusReason, that.statusReason);
  }

  @Override
  public int hashCode() {
    return Objects.hash(timestamp, taskState, statusMessage, statusReason);
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

  public Set<SingularityTaskHistoryUpdate> getPrevious() {
    return previous;
  }

  @Override public String toString() {
    return "SingularityTaskHistoryUpdate[" +
        "timestamp=" + timestamp +
        ", taskState=" + taskState +
        ", statusMessage=" + statusMessage +
        ", statusReason=" + statusReason +
        ", previous=" + previous +
        ']';
  }
}
