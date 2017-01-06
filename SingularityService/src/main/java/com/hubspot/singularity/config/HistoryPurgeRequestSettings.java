package com.hubspot.singularity.config;

import com.google.common.base.Optional;

public class HistoryPurgeRequestSettings {
  private Optional<Integer> deleteTaskHistoryAfterDays = Optional.absent();
  private Optional<Integer> deleteTaskHistoryAfterTasksPerRequest = Optional.absent();
  private Optional<Integer> deleteTaskHistoryBytesAfterDays = Optional.absent();
  private Optional<Integer> deleteTaskHistoryBytesAfterTasksPerRequest = Optional.absent();

  public Optional<Integer> getDeleteTaskHistoryAfterDays() {
    return deleteTaskHistoryAfterDays;
  }

  public HistoryPurgeRequestSettings setDeleteTaskHistoryAfterDays(Optional<Integer> deleteTaskHistoryAfterDays) {
    this.deleteTaskHistoryAfterDays = deleteTaskHistoryAfterDays;
    return this;
  }

  public Optional<Integer> getDeleteTaskHistoryAfterTasksPerRequest() {
    return deleteTaskHistoryAfterTasksPerRequest;
  }

  public void setDeleteTaskHistoryAfterTasksPerRequest(Optional<Integer> deleteTaskHistoryAfterTasksPerRequest) {
    this.deleteTaskHistoryAfterTasksPerRequest = deleteTaskHistoryAfterTasksPerRequest;
  }

  public Optional<Integer> getDeleteTaskHistoryBytesAfterDays() {
    return deleteTaskHistoryBytesAfterDays;
  }

  public void setDeleteTaskHistoryBytesAfterDays(Optional<Integer> deleteTaskHistoryBytesAfterDays) {
    this.deleteTaskHistoryBytesAfterDays = deleteTaskHistoryBytesAfterDays;
  }

  public Optional<Integer> getDeleteTaskHistoryBytesAfterTasksPerRequest() {
    return deleteTaskHistoryBytesAfterTasksPerRequest;
  }

  public void setDeleteTaskHistoryBytesAfterTasksPerRequest(Optional<Integer> deleteTaskHistoryBytesAfterTasksPerRequest) {
    this.deleteTaskHistoryBytesAfterTasksPerRequest = deleteTaskHistoryBytesAfterTasksPerRequest;
  }

  @Override
  public String toString() {
    return "HistoryPurgeRequestSettings{" +
      "deleteTaskHistoryAfterDays=" + deleteTaskHistoryAfterDays +
      ", deleteTaskHistoryAfterTasksPerRequest=" + deleteTaskHistoryAfterTasksPerRequest +
      ", deleteTaskHistoryBytesAfterDays=" + deleteTaskHistoryBytesAfterDays +
      ", deleteTaskHistoryBytesAfterTasksPerRequest=" + deleteTaskHistoryBytesAfterTasksPerRequest +
      '}';
  }
}
