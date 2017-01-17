package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularitySlaveUsage {

  private final long memoryBytesUsed;
  private final int numTasks;
  private final long timestamp;
  private final double cpusUsed;

  @JsonCreator
  public SingularitySlaveUsage(@JsonProperty("memoryBytesUsed") long memoryBytesUsed, @JsonProperty("timestamp") long timestamp, @JsonProperty("cpusUsed") double cpusUsed,
      @JsonProperty("numTasks") int numTasks) {
    this.memoryBytesUsed = memoryBytesUsed;
    this.timestamp = timestamp;
    this.cpusUsed = cpusUsed;
    this.numTasks = numTasks;
  }

  public long getMemoryBytesUsed() {
    return memoryBytesUsed;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public double getCpusUsed() {
    return cpusUsed;
  }

  public int getNumTasks() {
    return numTasks;
  }

  @Override
  public String toString() {
    return "SingularitySlaveUsage [memoryBytesUsed=" + memoryBytesUsed + ", numTasks=" + numTasks + ", timestamp=" + timestamp + ", cpusUsed=" + cpusUsed + "]";
  }

}
