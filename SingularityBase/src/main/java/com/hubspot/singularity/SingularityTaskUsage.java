package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityTaskUsage {

  private final long memoryRssBytes;
  private final double timestamp;
  private final double cpuSeconds;

  @JsonCreator
  public SingularityTaskUsage(@JsonProperty("memoryRssBytes") long memoryRssBytes, @JsonProperty("timestamp") double timestamp, @JsonProperty("cpuSeconds") double cpuSeconds) {
    this.memoryRssBytes = memoryRssBytes;
    this.timestamp = timestamp;
    this.cpuSeconds = cpuSeconds;
  }

  public long getMemoryRssBytes() {
    return memoryRssBytes;
  }

  public double getTimestamp() {
    return timestamp;
  }

  public double getCpuSeconds() {
    return cpuSeconds;
  }

  @Override
  public String toString() {
    return "SingularityTaskUsage [memoryRssBytes=" + memoryRssBytes + ", timestamp=" + timestamp + ", cpuSeconds=" + cpuSeconds + "]";
  }

}
