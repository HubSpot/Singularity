package com.hubspot.singularity.config;

import java.util.Collections;
import java.util.Map;

import com.google.common.base.Optional;

public class HistoryPurgingConfiguration {

  private int deleteTaskHistoryAfterDays = 0;

  private int deleteTaskHistoryAfterTasksPerRequest = 0;

  private int deleteTaskHistoryBytesAfterDays = 365;

  private int deleteTaskHistoryBytesAfterTasksPerRequest = 10000;

  private int checkTaskHistoryEveryHours = 24;

  private boolean enabled = false;

  private int purgeLimitPerQuery = 25000;

  private Map<String, HistoryPurgeRequestSettings> requestOverrides = Collections.emptyMap();

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

  public Optional<Integer> getDeleteTaskHistoryBytesAfterDays() {
    return absentIfNotOverOne(deleteTaskHistoryBytesAfterDays);
  }

  public void setDeleteTaskHistoryBytesAfterDays(int deleteTaskHistoryBytesAfterDays) {
    this.deleteTaskHistoryBytesAfterDays = deleteTaskHistoryBytesAfterDays;
  }

  public Optional<Integer> getDeleteTaskHistoryBytesAfterTasksPerRequest() {
    return absentIfNotOverOne(deleteTaskHistoryBytesAfterTasksPerRequest);
  }

  public void setDeleteTaskHistoryBytesAfterTasksPerRequest(int deleteTaskHistoryBytesAfterTasksPerRequest) {
    this.deleteTaskHistoryBytesAfterTasksPerRequest = deleteTaskHistoryBytesAfterTasksPerRequest;
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

  public Map<String, HistoryPurgeRequestSettings> getRequestOverrides() {
    return requestOverrides;
  }

  public void setRequestOverrides(Map<String, HistoryPurgeRequestSettings> requestOverrides) {
    this.requestOverrides = requestOverrides;
  }

  public int getPurgeLimitPerQuery() {
    return purgeLimitPerQuery;
  }

  public void setPurgeLimitPerQuery(int purgeLimitPerQuery) {
    this.purgeLimitPerQuery = purgeLimitPerQuery;
  }
}
