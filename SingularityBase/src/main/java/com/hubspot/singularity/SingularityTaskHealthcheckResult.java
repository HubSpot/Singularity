package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityTaskHealthcheckResult extends SingularityJsonObject {

  private final String taskId;
  private final Optional<Integer> statusCode;
  private final Optional<Long> durationMillis;
  private final Optional<String> responseBody;
  private final Optional<String> errorMessage;
  private final long timestamp;
  
  @JsonCreator
  public SingularityTaskHealthcheckResult(@JsonProperty("statusCode") Optional<Integer> statusCode, @JsonProperty("duration") Optional<Long> durationMillis, @JsonProperty("timestamp") long timestamp, 
      @JsonProperty("responseBody") Optional<String> responseBody, @JsonProperty("responseBody") Optional<String> errorMessage, @JsonProperty("taskId") String taskId) {
    this.statusCode = statusCode;
    this.errorMessage = errorMessage;
    this.durationMillis = durationMillis;
    this.timestamp = timestamp;
    this.responseBody = responseBody;
    this.taskId = taskId;
  }

  public Optional<Integer> getStatusCode() {
    return statusCode;
  }
  
  public Optional<Long> getDurationMillis() {
    return durationMillis;
  }
  
  public Optional<String> getErrorMessage() {
    return errorMessage;
  }

  public Long getTimestamp() {
    return timestamp;
  }
  
  public Optional<String> getResponseBody() {
    return responseBody;
  }
  
  public String getTaskId() {
    return taskId;
  }

  @Override
  public String toString() {
    return "SingularityTaskHealthcheckResult [statusCode=" + statusCode + ", durationMillis=" + durationMillis + ", timestamp=" + timestamp + ", responseBody=" + responseBody + ", errorMessage=" + errorMessage + ", taskId=" + taskId + "]";
  }
  
}
