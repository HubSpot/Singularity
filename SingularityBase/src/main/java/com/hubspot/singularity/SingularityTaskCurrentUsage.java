package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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

  public long getMemoryTotalBytes() {
    return memoryTotalBytes;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public double getCpusUsed() {
    return cpusUsed;
  }

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
