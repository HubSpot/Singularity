package com.hubspot.singularity;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.hubspot.mesos.JavaUtils;

public class SingularityDeployStatistics extends SingularityJsonObject {

  private final String requestId;
  private final String deployId;

  private final int numSuccess;
  private final int numFailures;

  private final int numSequentialRetries;

  private final List<Long> sequentialFailureTimestamps;
  
  private final Optional<Long> lastFinishAt;
  private final Optional<ExtendedTaskState> lastTaskState;

  public static SingularityDeployStatistics fromBytes(byte[] bytes, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(bytes, SingularityDeployStatistics.class);
    } catch (IOException e) {
      throw new SingularityJsonException(e);
    }
  }

  @JsonCreator
  public SingularityDeployStatistics(@JsonProperty("requestId") String requestId, @JsonProperty("deployId") String deployId, @JsonProperty("numSuccess") int numSuccess, @JsonProperty("numFailures") int numFailures,
      @JsonProperty("numSequentialRetries") int numSequentialRetries, @JsonProperty("lastFinishAt") Optional<Long> lastFinishAt, @JsonProperty("lastTaskState") Optional<ExtendedTaskState> lastTaskState,
      @JsonProperty("sequentialFailureTimestamps") List<Long> sequentialFailureTimestamps) {
    this.requestId = requestId;
    this.deployId = deployId;
    this.numSuccess = numSuccess;
    this.numFailures = numFailures;
    this.lastFinishAt = lastFinishAt;
    this.lastTaskState = lastTaskState;
    this.numSequentialRetries = numSequentialRetries;
    this.sequentialFailureTimestamps = JavaUtils.nonNullImmutable(sequentialFailureTimestamps);
  }

  public SingularityDeployStatisticsBuilder toBuilder() {
    return new SingularityDeployStatisticsBuilder(requestId, deployId)
      .setLastFinishAt(lastFinishAt)
      .setLastTaskState(lastTaskState)
      .setNumSequentialRetries(numSequentialRetries)
      .setNumFailures(numFailures)
      .setNumSuccess(numSuccess)
      .setSequentialFailureTimestamps(sequentialFailureTimestamps);
  }

  public String getRequestId() {
    return requestId;
  }

  public String getDeployId() {
    return deployId;
  }

  public int getNumSuccess() {
    return numSuccess;
  }

  public int getNumFailures() {
    return numFailures;
  }

  public Optional<Long> getLastFinishAt() {
    return lastFinishAt;
  }

  public Optional<ExtendedTaskState> getLastTaskState() {
    return lastTaskState;
  }

  public int getNumSequentialRetries() {
    return numSequentialRetries;
  }

  public List<Long> getSequentialFailureTimestamps() {
    return sequentialFailureTimestamps;
  }
  
  @Override
  public String toString() {
    return "SingularityDeployStatistics [requestId=" + requestId + ", deployId=" + deployId + ", numSuccess=" + numSuccess + ", numFailures=" + numFailures + ", numSequentialRetries=" + numSequentialRetries + 
        "sequentialFailureTimestamps=" + sequentialFailureTimestamps + ", lastFinishAt=" + lastFinishAt + ", lastTaskState=" + lastTaskState + "]";
  }

}
