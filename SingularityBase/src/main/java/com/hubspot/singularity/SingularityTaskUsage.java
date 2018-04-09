package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityTaskUsage {

  private final long memoryTotalBytes;
  private final double timestamp; // seconds
  private final double cpuSeconds;
  private final long diskTotalBytes;
  private final long cpusNrPeriods;
  private final long cpusNrThrottled;
  private final double cpusThrottledTimeSecs;

  @JsonCreator
  public SingularityTaskUsage(@JsonProperty("memoryTotalBytes") long memoryTotalBytes,
                              @JsonProperty("timestamp") double timestamp,
                              @JsonProperty("cpuSeconds") double cpuSeconds,
                              @JsonProperty("diskTotalBytes") long diskTotalBytes,
                              @JsonProperty("cpusNrPeriods") long cpusNrPeriods,
                              @JsonProperty("cpusNrThrottled") long cpusNrThrottled,
                              @JsonProperty("cpusThrottledTimeSecs") double cpusThrottledTimeSecs) {
    this.memoryTotalBytes = memoryTotalBytes;
    this.timestamp = timestamp;
    this.cpuSeconds = cpuSeconds;
    this.diskTotalBytes = diskTotalBytes;
    this.cpusNrPeriods = cpusNrPeriods;
    this.cpusNrThrottled = cpusNrThrottled;
    this.cpusThrottledTimeSecs = cpusThrottledTimeSecs;
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

  public long getCpusNrPeriods() {
    return cpusNrPeriods;
  }

  public long getCpusNrThrottled() {
    return cpusNrThrottled;
  }

  public double getCpusThrottledTimeSecs() {
    return cpusThrottledTimeSecs;
  }

  @Override
  public String toString() {
    return "SingularityTaskUsage{" +
        "memoryTotalBytes=" + memoryTotalBytes +
        ", timestamp=" + timestamp +
        ", cpuSeconds=" + cpuSeconds +
        ", diskTotalBytes=" + diskTotalBytes +
        ", cpusNrPeriods=" + cpusNrPeriods +
        ", cpusNrThrottled=" + cpusNrThrottled +
        ", cpusThrottledTimeSecs=" + cpusThrottledTimeSecs +
        '}';
  }
}
