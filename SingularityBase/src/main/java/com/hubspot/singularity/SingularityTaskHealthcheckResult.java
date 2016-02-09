package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ComparisonChain;
import com.hubspot.mesos.JavaUtils;

public class SingularityTaskHealthcheckResult extends SingularityTaskIdHolder implements Comparable<SingularityTaskHealthcheckResult> {

  private final Optional<Integer> statusCode;
  private final Optional<Long> durationMillis;
  private final Optional<String> responseBody;
  private final Optional<String> errorMessage;
  private final long timestamp;

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

  @Override
  public int compareTo(SingularityTaskHealthcheckResult o) {
    return ComparisonChain.start()
        .compare(timestamp, o.getTimestamp())
        .compare(o.getTaskId().getId(), getTaskId().getId())
        .result();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getTaskId(), statusCode, durationMillis, responseBody, errorMessage, timestamp);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
        return true;
    }
    if (other == null || other.getClass() != this.getClass()) {
        return false;
    }

    SingularityTaskHealthcheckResult that = (SingularityTaskHealthcheckResult) other;

    return Objects.equal(this.statusCode, that.statusCode)
            && Objects.equal(this.getTaskId(), that.getTaskId())
            && Objects.equal(this.durationMillis, that.durationMillis)
            && Objects.equal(this.responseBody, that.responseBody)
            && Objects.equal(this.errorMessage, that.errorMessage)
            && Objects.equal(this.timestamp, that.timestamp);
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

  public long getTimestamp() {
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
    return "SingularityTaskHealthcheckResult [statusCode=" + statusCode + ", durationMillis=" + durationMillis + ", timestamp=" + timestamp + ", responseBody="
        + responseBody + ", errorMessage=" + errorMessage + ", taskId=" + getTaskId() + "]";
  }

}
