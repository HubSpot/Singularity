package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A description of the current resource usage of a task")
public class SingularityTaskCurrentUsage {

  private final long memoryTotalBytes;
  private final long timestamp;
  private final double cpusUsed;
  private final long diskTotalBytes;

  @JsonCreator
  public SingularityTaskCurrentUsage(@JsonProperty("memoryTotalBytes") long memoryTotalBytes,
                                     @JsonProperty("long") long timestamp,
                                     @JsonProperty("cpusUsed") double cpusUsed,
                                     @JsonProperty("diskTotalBytes") long diskTotalBytes) {
    this.memoryTotalBytes = memoryTotalBytes;
    this.timestamp = timestamp;
    this.cpusUsed = cpusUsed;
    this.diskTotalBytes = diskTotalBytes;
  }

  @Schema(description = "The total memory used by the task in bytes")
  public long getMemoryTotalBytes() {
    return memoryTotalBytes;
  }

  @Schema(description = "The time at which this usage data was collected")
  public long getTimestamp() {
    return timestamp;
  }

  @Schema(description = "The cpus used by this task")
  public double getCpusUsed() {
    return cpusUsed;
  }

  @Schema(description = "The total disk usage for this task in bytes")
  public long getDiskTotalBytes() {
    return diskTotalBytes;
  }

  @Override
  public String toString() {
    return "SingularityTaskCurrentUsage{" +
        "memoryTotalBytes=" + memoryTotalBytes +
        ", timestamp=" + timestamp +
        ", cpusUsed=" + cpusUsed +
        ", diskTotalBytes=" + diskTotalBytes +
        '}';
  }
}
