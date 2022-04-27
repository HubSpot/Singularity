package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;

public class CrashLoopInfo {

  private final String requestId;
  private final String deployId;
  private final long start; // Not included in equals/hashCode
  private final Optional<Long> end; // Not included in equals/hashCode
  private final CrashLoopType type;

  @JsonCreator
  public CrashLoopInfo(
    @JsonProperty("requestId") String requestId,
    @JsonProperty("deployId") String deployId,
    @JsonProperty("start") long start,
    @JsonProperty("end") Optional<Long> end,
    @JsonProperty("type") CrashLoopType type
  ) {
    this.requestId = requestId;
    this.deployId = deployId;
    this.start = start;
    this.end = end;
    this.type = type;
  }

  public String getRequestId() {
    return requestId;
  }

  public String getDeployId() {
    return deployId;
  }

  public long getStart() {
    return start;
  }

  public Optional<Long> getEnd() {
    return end;
  }

  public CrashLoopType getType() {
    return type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    CrashLoopInfo info = (CrashLoopInfo) o;

    if (start != info.start) {
      return false;
    }
    if (requestId != null ? !requestId.equals(info.requestId) : info.requestId != null) {
      return false;
    }
    if (deployId != null ? !deployId.equals(info.deployId) : info.deployId != null) {
      return false;
    }
    if (end != null ? !end.equals(info.end) : info.end != null) {
      return false;
    }
    return type == info.type;
  }

  @Override
  public int hashCode() {
    int result = requestId != null ? requestId.hashCode() : 0;
    result = 31 * result + (deployId != null ? deployId.hashCode() : 0);
    result = 31 * result + (int) (start ^ (start >>> 32));
    result = 31 * result + (end != null ? end.hashCode() : 0);
    result = 31 * result + (type != null ? type.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return (
      "CrashLoopInfo{" +
      "requestId='" +
      requestId +
      '\'' +
      ", deployId='" +
      deployId +
      '\'' +
      ", start=" +
      start +
      ", end=" +
      end +
      ", type=" +
      type +
      '}'
    );
  }

  @JsonIgnore
  public boolean matches(CrashLoopInfo other) {
    return (
      requestId.equals(other.requestId) &&
      deployId.equals(other.deployId) &&
      type == other.type &&
      end.isPresent() == other.end.isPresent()
    );
  }
}
