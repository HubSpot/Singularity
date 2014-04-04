package com.hubspot.singularity;

import java.io.IOException;

import org.apache.mesos.Protos.TaskState;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

public class SingularityDeployStatistics extends SingularityJsonObject {

  private final String requestId;
  private final String deployId;
  
  private final int numSuccess;
  private final int numFailures;

  private final int numSequentialRetries;
  private final int numSequentialSuccess;
  private final int numSequentialFailures;
  
  private final Optional<Long> lastFinishAt;
  private final Optional<TaskState> lastTaskState;

  public static SingularityDeployStatistics fromBytes(byte[] bytes, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(bytes, SingularityDeployStatistics.class);
    } catch (IOException e) {
      throw new SingularityJsonException(e);
    }
  }
  
  @JsonCreator
  public SingularityDeployStatistics(@JsonProperty("requestId") String requestId, @JsonProperty("deployId") String deployId, @JsonProperty("numSuccess") int numSuccess, @JsonProperty("numFailures") int numFailures, 
      @JsonProperty("numSequentialRetries") int numSequentialRetries, @JsonProperty("lastFinishAt") Optional<Long> lastFinishAt, @JsonProperty("lastTaskState") Optional<TaskState> lastTaskState,
      @JsonProperty("numSequentialSuccess") int numSequentialSuccess, @JsonProperty("numSequentialFailures") int numSequentialFailures) {
    this.requestId = requestId;
    this.deployId = deployId;
    this.numSuccess = numSuccess;
    this.numFailures = numFailures;
    this.lastFinishAt = lastFinishAt;
    this.lastTaskState = lastTaskState;
    this.numSequentialRetries = numSequentialRetries;
    this.numSequentialFailures = numSequentialFailures;
    this.numSequentialSuccess = numSequentialSuccess;
  }

  public SingularityDeployStatisticsBuilder toBuilder() {
    return new SingularityDeployStatisticsBuilder(requestId, deployId)
      .setLastFinishAt(lastFinishAt)
      .setLastTaskState(lastTaskState)
      .setNumSequentialFailures(numSequentialFailures)
      .setNumSequentialRetries(numSequentialRetries)
      .setNumSequentialSuccess(numSequentialSuccess)
      .setNumFailures(numFailures)
      .setNumSuccess(numSuccess);
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

  public Optional<TaskState> getLastTaskState() {
    return lastTaskState;
  }

  public int getNumSequentialRetries() {
    return numSequentialRetries;
  }

  public int getNumSequentialSuccess() {
    return numSequentialSuccess;
  }

  public int getNumSequentialFailures() {
    return numSequentialFailures;
  }

  @Override
  public String toString() {
    return "SingularityDeployStatistics [requestId=" + requestId + ", deployId=" + deployId + ", numSuccess=" + numSuccess + ", numFailures=" + numFailures + ", numSequentialRetries=" + numSequentialRetries + ", numSequentialSuccess="
        + numSequentialSuccess + ", numSequentialFailures=" + numSequentialFailures + ", lastFinishAt=" + lastFinishAt + ", lastTaskState=" + lastTaskState + "]";
  }
  
}
