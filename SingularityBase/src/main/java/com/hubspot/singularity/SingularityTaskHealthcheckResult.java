package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.hubspot.mesos.JavaUtils;

public class SingularityTaskHealthcheckResult extends SingularityTaskIdHolder {

  private final Optional<Integer> statusCode;
  private final Optional<Long> durationMillis;
  private final Optional<String> responseBody;
  private final Optional<String> errorMessage;
  private final long timestamp;
  
  public static SingularityTaskHealthcheckResult fromBytes(byte[] bytes, ObjectMapper objectMapper) throws Exception {
    return objectMapper.readValue(bytes, SingularityTaskHealthcheckResult.class);
  }

  @JsonCreator
  public SingularityTaskHealthcheckResult(@JsonProperty("statusCode") Optional<Integer> statusCode, @JsonProperty("duration") Optional<Long> durationMillis, @JsonProperty("timestamp") long timestamp, 
      @JsonProperty("responseBody") Optional<String> responseBody, @JsonProperty("errorMessage") Optional<String> errorMessage, @JsonProperty("taskId") SingularityTaskId taskId) {
    super(taskId);
    
    this.statusCode = statusCode;
    this.errorMessage = errorMessage;
    this.durationMillis = durationMillis;
    this.timestamp = timestamp;
    this.responseBody = responseBody;
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
  
  @JsonIgnore
  public boolean isFailed() {
    return getErrorMessage().isPresent() || (getStatusCode().isPresent() && !JavaUtils.isHttpSuccess(getStatusCode().get()));
  }
  
  @Override
  public String toString() {
    return "SingularityTaskHealthcheckResult [statusCode=" + statusCode + ", durationMillis=" + durationMillis + ", timestamp=" + timestamp + ", responseBody=" + responseBody + ", errorMessage=" + errorMessage + ", taskId=" + getTaskId() + "]";
  }
  
}
