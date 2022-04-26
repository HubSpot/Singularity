package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(
  description = "The number of tasks in each state for a particular request. Cleaning tasks are also included in active (i.e. total task count = active + pending)"
)
public class SingularityTaskCounts {

  private final String requestId;
  private final int active;
  private final int pending;
  private final int cleaning;

  @JsonCreator
  public SingularityTaskCounts(
    @JsonProperty("requestId") String requestId,
    @JsonProperty("active") int active,
    @JsonProperty("pending") int pending,
    @JsonProperty("cleaning") int cleaning
  ) {
    this.requestId = requestId;
    this.active = active;
    this.pending = pending;
    this.cleaning = cleaning;
  }

  @Schema(description = "Request id")
  public String getRequestId() {
    return requestId;
  }

  @Schema(description = "The number of actively running tasks")
  public int getActive() {
    return active;
  }

  @Schema(description = "The number of tasks that have not yet launched")
  public int getPending() {
    return pending;
  }

  @Schema(
    description = "The number of tasks in a cleaning state. Note these are also counted under 'active'"
  )
  public int getCleaning() {
    return cleaning;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityTaskCounts that = (SingularityTaskCounts) o;
    return (
      active == that.active &&
      pending == that.pending &&
      cleaning == that.cleaning &&
      Objects.equals(requestId, that.requestId)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(requestId, active, pending, cleaning);
  }

  @Override
  public String toString() {
    return (
      "SingularityTaskCounts{" +
      "requestId='" +
      requestId +
      '\'' +
      ", active=" +
      active +
      ", pending=" +
      pending +
      ", cleaning=" +
      cleaning +
      '}'
    );
  }
}
