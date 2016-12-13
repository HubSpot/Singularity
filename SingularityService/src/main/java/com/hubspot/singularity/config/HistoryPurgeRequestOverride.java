package com.hubspot.singularity.config;

import com.google.common.base.Optional;

public class HistoryPurgeRequestOverride {
  private Optional<Integer> deleteTaskHistoryAfterDays = Optional.absent();
  private Optional<Integer> deleteTaskHistoryAfterTasksPerRequest = Optional.absent();
  private Optional<Boolean> deleteTaskHistoryBytesInsteadOfEntireRow = Optional.absent();

  public Optional<Integer> getDeleteTaskHistoryAfterDays() {
    return deleteTaskHistoryAfterDays;
  }

  public HistoryPurgeRequestOverride setDeleteTaskHistoryAfterDays(Optional<Integer> deleteTaskHistoryAfterDays) {
    this.deleteTaskHistoryAfterDays = deleteTaskHistoryAfterDays;
    return this;
  }

  public Optional<Integer> getDeleteTaskHistoryAfterTasksPerRequest() {
    return deleteTaskHistoryAfterTasksPerRequest;
  }

  public HistoryPurgeRequestOverride setDeleteTaskHistoryAfterTasksPerRequest(Optional<Integer> deleteTaskHistoryAfterTasksPerRequest) {
    this.deleteTaskHistoryAfterTasksPerRequest = deleteTaskHistoryAfterTasksPerRequest;
    return this;
  }

  public Optional<Boolean> getDeleteTaskHistoryBytesInsteadOfEntireRow() {
    return deleteTaskHistoryBytesInsteadOfEntireRow;
  }

  public HistoryPurgeRequestOverride setDeleteTaskHistoryBytesInsteadOfEntireRow(Optional<Boolean> deleteTaskHistoryBytesInsteadOfEntireRow) {
    this.deleteTaskHistoryBytesInsteadOfEntireRow = deleteTaskHistoryBytesInsteadOfEntireRow;
    return this;
  }
}
