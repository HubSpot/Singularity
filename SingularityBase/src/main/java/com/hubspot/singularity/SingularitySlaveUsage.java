package com.hubspot.singularity;

import java.util.Map;

import com.google.common.base.Optional;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularitySlaveUsage {

  public enum ResourceUsageType {
    CPU_USED, MEMORY_BYTES_USED, CPU_FREE, MEMORY_BYTES_FREE
  }

  public static final long BYTES_PER_MEGABYTE = 1024L * 1024L;

  private final long memoryBytesUsed;
  private final long memoryMbReserved;
  private final int numTasks;
  private final long timestamp;
  private final double cpusUsed;
  private final double cpusReserved;
  private final Optional<Long> memoryMbTotal;
  private final Optional<Double> cpusTotal;
  private final Map<ResourceUsageType, Number> longRunningTasksUsage;

  @JsonCreator
  public SingularitySlaveUsage(@JsonProperty("memoryBytesUsed") long memoryBytesUsed,
                               @JsonProperty("memoryMbReserved") long memoryMbReserved,
                               @JsonProperty("timestamp") long timestamp,
                               @JsonProperty("cpusUsed") double cpusUsed,
                               @JsonProperty("cpusReserved") double cpusReserved,
                               @JsonProperty("numTasks") int numTasks,
                               @JsonProperty("memoryMbTotal") Optional<Long> memoryMbTotal,
                               @JsonProperty("cpusTotal") Optional<Double> cpusTotal,
                               @JsonProperty("longRunningTasksUsage") Map<ResourceUsageType, Number> longRunningTasksUsage) {
    this.memoryBytesUsed = memoryBytesUsed;
    this.memoryMbReserved = memoryMbReserved;
    this.timestamp = timestamp;
    this.cpusUsed = cpusUsed;
    this.cpusReserved = cpusReserved;
    this.numTasks = numTasks;
    this.memoryMbTotal = memoryMbTotal;
    this.cpusTotal = cpusTotal;
    this.longRunningTasksUsage = longRunningTasksUsage;
  }

  public long getMemoryBytesUsed() {
    return memoryBytesUsed;
  }

  public long getMemoryMbReserved() {
    return memoryMbReserved;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public double getCpusUsed() {
    return cpusUsed;
  }

  public double getCpusReserved() {
    return cpusReserved;
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

  public Optional<Double> getCpusTotal() {
    return cpusTotal;
  }

  public Map<ResourceUsageType, Number> getLongRunningTasksUsage() {
    return longRunningTasksUsage;
  }

  @Override
  public String toString() {
    return "SingularitySlaveUsage [memoryBytesUsed=" + memoryBytesUsed +
        ", memoryMbReserved=" + memoryMbReserved +
        ", memoryMbTotal=" + memoryMbTotal +
        ", cpusUsed=" + cpusUsed +
        ", cpusReserved=" + cpusReserved +
        ", cpusTotal=" + cpusTotal +
        ", numTasks=" + numTasks +
        ", longRunningTasksUsage=" + longRunningTasksUsage +
        ", timestamp=" + timestamp +
        "]";
  }
}
