package com.hubspot.singularity;

import org.apache.mesos.Protos.TaskState;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
  
  private final Optional<Long> lastStartAt;
  private final Optional<Long> lastFinishAt;
  private final Optional<String> lastTaskStatus;
  
  public static SingularityDeployStatistics fromBytes(byte[] bytes, ObjectMapper objectMapper) throws Exception {
    return objectMapper.readValue(bytes, SingularityDeployStatistics.class);
  }
  
  @JsonCreator
  public SingularityDeployStatistics(@JsonProperty("requestId") String requestId, @JsonProperty("deployId") String deployId, @JsonProperty("numSuccess") int numSuccess, @JsonProperty("numFailures") int numFailures, 
      @JsonProperty("numSequentialRetries") int numSequentialRetries, @JsonProperty("lastStartAt") Optional<Long> lastStartAt, @JsonProperty("lastFinishAt") Optional<Long> lastFinishAt, @JsonProperty("lastTaskStatus") Optional<String> lastTaskStatus,
      @JsonProperty("numSequentialSuccess") int numSequentialSuccess, @JsonProperty("numSequentialFailures") int numSequentialFailures) {
    this.requestId = requestId;
    this.deployId = deployId;
    this.numSuccess = numSuccess;
    this.numFailures = numFailures;
    this.numSequentialRetries = numSequentialRetries;
    this.lastStartAt = lastStartAt;
    this.lastFinishAt = lastFinishAt;
    this.lastTaskStatus = lastTaskStatus;
    this.numSequentialFailures = numSequentialFailures;
    this.numSequentialSuccess = numSequentialFailures;
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

  public Optional<Long> getLastStartAt() {
    return lastStartAt;
  }

  public Optional<Long> getLastFinishAt() {
    return lastFinishAt;
  }

  public Optional<String> getLastTaskStatus() {
    return lastTaskStatus;
  }
  
  @JsonIgnore
  public Optional<TaskState> getLastTaskStatusEnum() {
    if (lastTaskStatus.isPresent()) {
      return Optional.of(TaskState.valueOf(lastTaskStatus.get()));
    }
    return Optional.absent();
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
        + numSequentialSuccess + ", numSequentialFailures=" + numSequentialFailures + ", lastStartAt=" + lastStartAt + ", lastFinishAt=" + lastFinishAt + ", lastTaskStatus=" + lastTaskStatus + "]";
  }
  
}
