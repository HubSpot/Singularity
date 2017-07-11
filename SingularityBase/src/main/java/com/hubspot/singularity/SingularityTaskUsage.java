package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityTaskUsage {

  private final long memoryTotalBytes;
  private final double timestampSeconds;
  private final double cpuSeconds;

  @JsonCreator
  public SingularityTaskUsage(@JsonProperty("memoryTotalBytes") long memoryTotalBytes, @JsonProperty("timestampSeconds") double timestampSeconds, @JsonProperty("cpuSeconds") double cpuSeconds) {
    this.memoryTotalBytes = memoryTotalBytes;
    this.timestampSeconds = timestampSeconds;
    this.cpuSeconds = cpuSeconds;
  }

  public long getMemoryTotalBytes() {
    return memoryTotalBytes;
  }

  public double getTimestampSeconds() {
    return timestampSeconds;
  }

  public double getCpuSeconds() {
    return cpuSeconds;
  }

  @Override
  public String toString() {
    return "SingularityTaskUsage [memoryTotalBytes=" + memoryTotalBytes + ", timestampSeconds=" + timestampSeconds + ", cpuSeconds=" + cpuSeconds + "]";
  }

}
