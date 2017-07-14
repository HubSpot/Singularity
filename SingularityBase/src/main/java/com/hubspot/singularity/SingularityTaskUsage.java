package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityTaskUsage {

  private final long memoryTotalBytes;
  private final double timestamp; // seconds
  private final double cpuSeconds;

  @JsonCreator
  public SingularityTaskUsage(@JsonProperty("memoryTotalBytes") long memoryTotalBytes, @JsonProperty("timestamp") double timestamp, @JsonProperty("cpuSeconds") double cpuSeconds) {
    this.memoryTotalBytes = memoryTotalBytes;
    this.timestamp = timestamp;
    this.cpuSeconds = cpuSeconds;
  }

  public long getMemoryTotalBytes() {
    return memoryTotalBytes;
  }

  public double getTimestamp() {
    return timestamp;
  }

  public double getCpuSeconds() {
    return cpuSeconds;
  }

  @Override
  public String toString() {
    return "SingularityTaskUsage [memoryTotalBytes=" + memoryTotalBytes + ", timestamp=" + timestamp + ", cpuSeconds=" + cpuSeconds + "]";
  }

}
