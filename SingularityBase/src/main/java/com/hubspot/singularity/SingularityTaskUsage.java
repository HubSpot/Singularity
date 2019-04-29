package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Current usage for a task")
public class SingularityTaskUsage {

  private final long memoryTotalBytes;
  private final long timestamp; // epoch millis
  private final double cpuSeconds;
  private final long diskTotalBytes;
  private final long cpusNrPeriods;
  private final long cpusNrThrottled;
  private final double cpusThrottledTimeSecs;

  @JsonCreator
  public SingularityTaskUsage(@JsonProperty("memoryTotalBytes") long memoryTotalBytes,
                              @JsonProperty("timestamp") long timestamp,
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

  @Schema(description = "Current total memory usage in bytes")
  public long getMemoryTotalBytes() {
    return memoryTotalBytes;
  }

  @Schema(description = "Timestamp this usage was recorded (epoch millis)")
  public long getTimestamp() {
    return timestamp;
  }

  @Schema(description = "Seconds of cpu time consumed by this task")
  public double getCpuSeconds() {
    return cpuSeconds;
  }

  @Schema(description = "Total disk usage in bytes for this task")
  public long getDiskTotalBytes() {
    return diskTotalBytes;
  }

  @Schema(description = "Number of cpu periods used by this task (from cgroups)")
  public long getCpusNrPeriods() {
    return cpusNrPeriods;
  }

  @Schema(description = "Number of cpu periods throttled for this task (from cgroups)")
  public long getCpusNrThrottled() {
    return cpusNrThrottled;
  }

  @Schema(description = "Total cpu time throttled for this task(from cgroups)")
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
