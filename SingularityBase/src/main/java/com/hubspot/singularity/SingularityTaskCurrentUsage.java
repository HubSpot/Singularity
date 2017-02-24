package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityTaskCurrentUsage {

  private final long memoryRssBytes;
  private final long timestamp;
  private final double cpusUsed;

  @JsonCreator
  public SingularityTaskCurrentUsage(@JsonProperty("memoryRssBytes") long memoryRssBytes, @JsonProperty("long") long timestamp, @JsonProperty("cpusUsed") double cpusUsed) {
    this.memoryRssBytes = memoryRssBytes;
    this.timestamp = timestamp;
    this.cpusUsed = cpusUsed;
  }

  public long getMemoryRssBytes() {
    return memoryRssBytes;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public double getCpusUsed() {
    return cpusUsed;
  }

  @Override
  public String toString() {
    return "SingularityTaskCurrentUsage [memoryRssBytes=" + memoryRssBytes + ", timestamp=" + timestamp + ", cpusUsed=" + cpusUsed + "]";
  }

}
