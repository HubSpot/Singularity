package com.hubspot.singularity.config;

import java.util.Collections;
import java.util.Map;

import com.google.common.base.Optional;

public class HistoryPurgingConfiguration {

  private int deleteTaskHistoryAfterDays = 365;

  private int deleteTaskHistoryAfterTasksPerRequest = 10000;

  private boolean deleteTaskHistoryBytesInsteadOfEntireRow = true;

  private int checkTaskHistoryEveryHours = 24;

  private boolean enabled = false;

  private Map<String, HistoryPurgeRequestOverride> requestOverrides = Collections.emptyMap();

  private Optional<Integer> absentIfNotOverOne(int value) {
    if (value < 1) {
      return Optional.absent();
    }
    return Optional.of(value);
  }

  public Optional<Integer> getDeleteTaskHistoryAfterDays() {
    return absentIfNotOverOne(deleteTaskHistoryAfterDays);
  }

  public void setDeleteTaskHistoryAfterDays(int deleteTaskHistoryAfterDays) {
    this.deleteTaskHistoryAfterDays = deleteTaskHistoryAfterDays;
  }

  public Optional<Integer> getDeleteTaskHistoryAfterTasksPerRequest() {
    return absentIfNotOverOne(deleteTaskHistoryAfterTasksPerRequest);
  }

  public void setDeleteTaskHistoryAfterTasksPerRequest(int deleteTaskHistoryAfterTasksPerRequest) {
    this.deleteTaskHistoryAfterTasksPerRequest = deleteTaskHistoryAfterTasksPerRequest;
  }

  public boolean isDeleteTaskHistoryBytesInsteadOfEntireRow() {
    return deleteTaskHistoryBytesInsteadOfEntireRow;
  }

  public void setDeleteTaskHistoryBytesInsteadOfEntireRow(boolean deleteTaskHistoryBytesInsteadOfEntireRow) {
    this.deleteTaskHistoryBytesInsteadOfEntireRow = deleteTaskHistoryBytesInsteadOfEntireRow;
  }

  public int getCheckTaskHistoryEveryHours() {
    return checkTaskHistoryEveryHours;
  }

  public void setCheckTaskHistoryEveryHours(int checkTaskHistoryEveryHours) {
    this.checkTaskHistoryEveryHours = checkTaskHistoryEveryHours;
  }

  public boolean isEnabledAndValid() {
    return enabled && checkTaskHistoryEveryHours > 0 && (getDeleteTaskHistoryAfterDays().isPresent() || getDeleteTaskHistoryAfterTasksPerRequest().isPresent());
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Map<String, HistoryPurgeRequestOverride> getRequestOverrides() {
    return requestOverrides;
  }

  public void setRequestOverrides(Map<String, HistoryPurgeRequestOverride> requestOverrides) {
    this.requestOverrides = requestOverrides;
  }
}
