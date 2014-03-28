package com.hubspot.singularity;

import com.google.common.base.Optional;

public class SingularityDeployStatisticsBuilder extends SingularityJsonObject {

  private final String requestId;
  private final String deployId;
  
  private int numSuccess;
  private int numFailures;

  private int numSequentialRetries;
  private int numSequentialSuccess;
  private int numSequentialFailures;
  
  private Optional<Long> lastFinishAt;
  private Optional<String> lastTaskStatus;
  
  public SingularityDeployStatisticsBuilder(String requestId, String deployId) {
    this.requestId = requestId;
    this.deployId = deployId;
  }
  
  public SingularityDeployStatistics build() {
    return new SingularityDeployStatistics(requestId, deployId, numSuccess, numFailures, numSequentialRetries, lastFinishAt, lastTaskStatus, numSequentialSuccess, numSequentialFailures);
  }
  
  public int getNumSuccess() {
    return numSuccess;
  }

  public SingularityDeployStatisticsBuilder setNumSuccess(int numSuccess) {
    this.numSuccess = numSuccess;
    return this;
  }

  public int getNumFailures() {
    return numFailures;
  }

  public SingularityDeployStatisticsBuilder setNumFailures(int numFailures) {
    this.numFailures = numFailures;
    return this;
  }

  public int getNumSequentialRetries() {
    return numSequentialRetries;
  }

  public SingularityDeployStatisticsBuilder setNumSequentialRetries(int numSequentialRetries) {
    this.numSequentialRetries = numSequentialRetries;
    return this;
  }

  public int getNumSequentialSuccess() {
    return numSequentialSuccess;
  }

  public SingularityDeployStatisticsBuilder setNumSequentialSuccess(int numSequentialSuccess) {
    this.numSequentialSuccess = numSequentialSuccess;
    return this;
  }

  public int getNumSequentialFailures() {
    return numSequentialFailures;
  }

  public SingularityDeployStatisticsBuilder setNumSequentialFailures(int numSequentialFailures) {
    this.numSequentialFailures = numSequentialFailures;
    return this;
  }

  public Optional<Long> getLastFinishAt() {
    return lastFinishAt;
  }

  public SingularityDeployStatisticsBuilder setLastFinishAt(Optional<Long> lastFinishAt) {
    this.lastFinishAt = lastFinishAt;
    return this;
  }

  public Optional<String> getLastTaskStatus() {
    return lastTaskStatus;
  }

  public SingularityDeployStatisticsBuilder setLastTaskStatus(Optional<String> lastTaskStatus) {
    this.lastTaskStatus = lastTaskStatus;
    return this;
  }

  public String getRequestId() {
    return requestId;
  }

  public String getDeployId() {
    return deployId;
  }

  @Override
  public String toString() {
    return "SingularityDeployStatisticsBuilder [requestId=" + requestId + ", deployId=" + deployId + ", numSuccess=" + numSuccess + ", numFailures=" + numFailures + ", numSequentialRetries=" + numSequentialRetries
        + ", numSequentialSuccess=" + numSequentialSuccess + ", numSequentialFailures=" + numSequentialFailures + ", lastFinishAt=" + lastFinishAt + ", lastTaskStatus=" + lastTaskStatus + "]";
  }

}
