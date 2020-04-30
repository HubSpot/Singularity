package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ComparisonChain;
import com.hubspot.mesos.JavaUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import java.util.Optional;

@Schema(description = "A Healthcheck result for a particular task")
public class SingularityTaskHealthcheckResult
  extends SingularityTaskIdHolder
  implements Comparable<SingularityTaskHealthcheckResult> {
  private final Optional<Integer> statusCode;
  private final Optional<Long> durationMillis;
  private final Optional<String> responseBody;
  private final Optional<String> errorMessage;
  private final boolean startup;
  private final long timestamp;

  @JsonCreator
  public SingularityTaskHealthcheckResult(
    @JsonProperty("statusCode") Optional<Integer> statusCode,
    @JsonProperty("duration") Optional<Long> durationMillis,
    @JsonProperty("timestamp") long timestamp,
    @JsonProperty("responseBody") Optional<String> responseBody,
    @JsonProperty("errorMessage") Optional<String> errorMessage,
    @JsonProperty("taskId") SingularityTaskId taskId,
    @JsonProperty("startup") Optional<Boolean> startup
  ) {
    super(taskId);
    this.statusCode = statusCode;
    this.errorMessage = errorMessage;
    this.durationMillis = durationMillis;
    this.timestamp = timestamp;
    this.responseBody = responseBody;
    this.startup = startup.orElse(false);
  }

  @Override
  public int compareTo(SingularityTaskHealthcheckResult o) {
    return ComparisonChain
      .start()
      .compare(timestamp, o.getTimestamp())
      .compare(o.getTaskId().getId(), getTaskId().getId())
      .result();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityTaskHealthcheckResult that = (SingularityTaskHealthcheckResult) o;
    return (
      startup == that.startup &&
      timestamp == that.timestamp &&
      Objects.equals(statusCode, that.statusCode) &&
      Objects.equals(durationMillis, that.durationMillis) &&
      Objects.equals(responseBody, that.responseBody) &&
      Objects.equals(errorMessage, that.errorMessage)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      statusCode,
      durationMillis,
      responseBody,
      errorMessage,
      startup,
      timestamp
    );
  }

  @Schema(description = "Status code if a response was received", nullable = true)
  public Optional<Integer> getStatusCode() {
    return statusCode;
  }

  @Schema(
    description = "Response time of the check if a response was received",
    nullable = true
  )
  public Optional<Long> getDurationMillis() {
    return durationMillis;
  }

  @Schema(description = "Error message if the check failed", nullable = true)
  public Optional<String> getErrorMessage() {
    return errorMessage;
  }

  @Schema(description = "Timestamp of this healthcheck", nullable = true)
  public long getTimestamp() {
    return timestamp;
  }

  @Schema(description = "response body if a response was received", nullable = true)
  public Optional<String> getResponseBody() {
    return responseBody;
  }

  @Schema(description = "`true` if the healthcheck was running during the startup phase")
  public boolean isStartup() {
    return startup;
  }

  @JsonIgnore
  public boolean isFailed() {
    return (
      getErrorMessage().isPresent() ||
      (getStatusCode().isPresent() && !JavaUtils.isHttpSuccess(getStatusCode().get()))
    );
  }

  @Override
  public String toString() {
    return (
      "SingularityTaskHealthcheckResult{" +
      "statusCode=" +
      statusCode +
      ", durationMillis=" +
      durationMillis +
      ", responseBody=" +
      responseBody +
      ", errorMessage=" +
      errorMessage +
      ", startup=" +
      startup +
      ", timestamp=" +
      timestamp +
      "} " +
      super.toString()
    );
  }
}
