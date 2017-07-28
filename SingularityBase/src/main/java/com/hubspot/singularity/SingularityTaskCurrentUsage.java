package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityTaskCurrentUsage {

  private final long memoryTotalBytes;
  private final long timestamp;
  private final double cpusUsed;

  @JsonCreator
  public SingularityTaskCurrentUsage(@JsonProperty("memoryTotalBytes") long memoryTotalBytes, @JsonProperty("long") long timestamp, @JsonProperty("cpusUsed") double cpusUsed) {
    this.memoryTotalBytes = memoryTotalBytes;
    this.timestamp = timestamp;
    this.cpusUsed = cpusUsed;
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

  @Override
  public String toString() {
    return "SingularityTaskCurrentUsage [memoryTotalBytes=" + memoryTotalBytes + ", timestamp=" + timestamp + ", cpusUsed=" + cpusUsed + "]";
  }

}
