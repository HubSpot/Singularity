package com.hubspot.singularity;

import java.util.Map;

import com.google.common.base.Optional;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularitySlaveUsage {

  public enum ResourceUsageType {
    CPU_USED, MEMORY_BYTES_USED
  }

  public static final long BYTES_PER_MEGABYTE = 1024L * 1024L;

  private final long memoryBytesUsed;
  private final int numTasks;
  private final long timestamp;
  private final double cpusUsed;
  private final Optional<Long> memoryMbTotal;
  private final Optional<Double> cpuTotal;
  private final Map<ResourceUsageType, Number> longRunningTasksUsage;

  @JsonCreator
  public SingularitySlaveUsage(@JsonProperty("memoryBytesUsed") long memoryBytesUsed,
                               @JsonProperty("timestamp") long timestamp,
                               @JsonProperty("cpusUsed") double cpusUsed,
                               @JsonProperty("numTasks") int numTasks,
                               @JsonProperty("memoryMbTotal") Optional<Long> memoryMbTotal,
                               @JsonProperty("cpuTotal") Optional<Double> cpuTotal,
                               @JsonProperty("longRunningTasksUsage") Map<ResourceUsageType, Number> longRunningTasksUsage) {
    this.memoryBytesUsed = memoryBytesUsed;
    this.timestamp = timestamp;
    this.cpusUsed = cpusUsed;
    this.numTasks = numTasks;
    this.memoryMbTotal = memoryMbTotal;
    this.cpuTotal = cpuTotal;
    this.longRunningTasksUsage = longRunningTasksUsage;
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

  public Optional<Long> getMemoryBytesTotal() {
    return memoryMbTotal.isPresent() ? Optional.of(memoryMbTotal.get() * BYTES_PER_MEGABYTE) : Optional.absent();
  }

  public Optional<Long> getMemoryMbTotal() {
    return memoryMbTotal.isPresent() ? Optional.of(memoryMbTotal.get()) : Optional.absent();
  }

  public Optional<Double> getCpuTotal() {
    return cpuTotal;
  }

  public Map<ResourceUsageType, Number> getLongRunningTasksUsage() {
    return longRunningTasksUsage;
  }

  @Override
  public String toString() {
    return "SingularitySlaveUsage [memoryBytesUsed=" + memoryBytesUsed + ", numTasks=" + numTasks + ", timestamp=" + timestamp + ", cpusUsed=" + cpusUsed + "]";
  }

}
