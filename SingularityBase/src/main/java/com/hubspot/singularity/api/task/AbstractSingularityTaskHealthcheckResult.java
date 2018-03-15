package com.hubspot.singularity.api.task;

import java.util.Optional;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ComparisonChain;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.annotations.SingularityStyle;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema(description = "A Healthcheck result for a particular task")
public abstract class AbstractSingularityTaskHealthcheckResult implements Comparable<SingularityTaskHealthcheckResult>, SingularityTaskIdHolder {
  @Schema(description = "Status code if a response was received", nullable = true)
  public abstract Optional<Integer> getStatusCode();

  @JsonProperty("duration")
  @Schema(description = "Response time of the check if a response was received", nullable = true)
  public abstract Optional<Long> getDurationMillis();

  @Schema(description = "Timestamp of this healthcheck", nullable = true)
  public abstract long getTimestamp();

  @Schema(description = "response body if a response was received", nullable = true)
  public abstract Optional<String> getResponseBody();

  @Schema(description = "Error message if the check failed", nullable = true)
  public abstract Optional<String> getErrorMessage();

  @Schema(description = "Task Id")
  public abstract SingularityTaskId getTaskId();

  @Default
  @Schema(description = "`true` if the healthcheck was running during the startup phase")
  public boolean isStartup() {
    return false;
  }

  @JsonIgnore
  public boolean isFailed() {
    return getErrorMessage().isPresent() || (getStatusCode().isPresent() && !JavaUtils.isHttpSuccess(getStatusCode().get()));
  }

  @Override
  public int compareTo(SingularityTaskHealthcheckResult o) {
    return ComparisonChain.start()
        .compare(getTimestamp(), o.getTimestamp())
        .compare(o.getTaskId().getId(), getTaskId().getId())
        .result();
  }
}
