package com.hubspot.singularity;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.ComparisonChain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Uniquely identifies a deploy")
public class SingularityDeployMarker implements Comparable<SingularityDeployMarker> {

  private final String requestId;
  private final String deployId;
  private final long timestamp;
  private final Optional<String> user;
  private final Optional<String> message;

  @JsonCreator
  public SingularityDeployMarker(@JsonProperty("requestId") String requestId, @JsonProperty("deployId") String deployId, @JsonProperty("timestamp") long timestamp,
      @JsonProperty("user") Optional<String> user, @JsonProperty("message") Optional<String> message) {
    this.requestId = requestId;
    this.deployId = deployId;
    this.timestamp = timestamp;
    this.user = user;
    this.message = message;
  }

  @Override
  public int compareTo(SingularityDeployMarker o) {
    return ComparisonChain.start()
        .compare(timestamp, o.getTimestamp())
        .compare(deployId, o.getDeployId())
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
    SingularityDeployMarker that = (SingularityDeployMarker) o;
    return Objects.equals(requestId, that.requestId) &&
        Objects.equals(deployId, that.deployId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(requestId, deployId);
  }

  @Schema(description = "The request associated with thsi deploy")
  public String getRequestId() {
    return requestId;
  }

  @Schema(description = "A unique ID for this deploy")
  public String getDeployId() {
    return deployId;
  }

  @Schema(description = "The time this deploy was created")
  public long getTimestamp() {
    return timestamp;
  }

  @Schema(description = "The user associated with this deploy", nullable = true)
  public Optional<String> getUser() {
    return user;
  }

  @Schema(description = "An optional message associated with this deploy", nullable = true)
  public Optional<String> getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return "SingularityDeployMarker{" +
        "requestId='" + requestId + '\'' +
        ", deployId='" + deployId + '\'' +
        ", timestamp=" + timestamp +
        ", user=" + user +
        ", message=" + message +
        '}';
  }
}
