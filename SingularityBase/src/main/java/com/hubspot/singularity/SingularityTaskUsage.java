package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityTaskUsage {

  private final long memoryTotalBytes;
  private final double timestamp; // seconds
  private final double cpuSeconds;
  private final long diskTotalBytes;

  @JsonCreator
  public SingularityTaskUsage(@JsonProperty("memoryTotalBytes") long memoryTotalBytes,
                              @JsonProperty("timestamp") double timestamp,
                              @JsonProperty("cpuSeconds") double cpuSeconds,
                              @JsonProperty("diskTotalBytes") long diskTotalBytes) {
    this.memoryTotalBytes = memoryTotalBytes;
    this.timestamp = timestamp;
    this.cpuSeconds = cpuSeconds;
    this.diskTotalBytes = diskTotalBytes;
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

  public long getDiskTotalBytes() {
    return diskTotalBytes;
  }

  @Override
  public String toString() {
    return "SingularityTaskUsage [memoryTotalBytes=" + memoryTotalBytes + ", timestamp=" + timestamp + ", cpuSeconds=" + cpuSeconds + "]";
  }

}
