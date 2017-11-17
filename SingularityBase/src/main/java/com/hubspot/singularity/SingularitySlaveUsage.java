package com.hubspot.singularity;

import java.util.Map;

import com.google.common.base.Optional;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularitySlaveUsage {

  public enum ResourceUsageType {
    CPU_USED, MEMORY_BYTES_USED, DISK_BYTES_USED, CPU_FREE, MEMORY_BYTES_FREE, DISK_BYTES_FREE
  }

  public static final long BYTES_PER_MEGABYTE = 1000L * 1000L;

  private final double cpusUsed;
  private final double cpusReserved;
  private final Optional<Double> cpusTotal;
  private final long memoryBytesUsed;
  private final long memoryMbReserved;
  private final Optional<Long> memoryMbTotal;
  private final long diskBytesUsed;
  private final long diskMbReserved;
  private final Optional<Long> diskMbTotal;
  private final Map<ResourceUsageType, Number> longRunningTasksUsage;
  private final int numTasks;
  private final long timestamp;

  @JsonCreator
  public SingularitySlaveUsage(@JsonProperty("cpusUsed") double cpusUsed,
                               @JsonProperty("cpusReserved") double cpusReserved,
                               @JsonProperty("cpusTotal") Optional<Double> cpusTotal,
                               @JsonProperty("memoryBytesUsed") long memoryBytesUsed,
                               @JsonProperty("memoryMbReserved") long memoryMbReserved,
                               @JsonProperty("memoryMbTotal") Optional<Long> memoryMbTotal,
                               @JsonProperty("diskBytesUsed") long diskBytesUsed,
                               @JsonProperty("diskMbReserved") long diskMbReserved,
                               @JsonProperty("diskMbTotal") Optional<Long> diskMbTotal,
                               @JsonProperty("longRunningTasksUsage") Map<ResourceUsageType, Number> longRunningTasksUsage,
                               @JsonProperty("numTasks") int numTasks,
                               @JsonProperty("timestamp") long timestamp) {
    this.cpusUsed = cpusUsed;
    this.cpusReserved = cpusReserved;
    this.cpusTotal = cpusTotal;
    this.memoryBytesUsed = memoryBytesUsed;
    this.memoryMbReserved = memoryMbReserved;
    this.memoryMbTotal = memoryMbTotal;
    this.diskBytesUsed = diskBytesUsed;
    this.diskMbReserved = diskMbReserved;
    this.diskMbTotal = diskMbTotal;
    this.longRunningTasksUsage = longRunningTasksUsage;
    this.numTasks = numTasks;
    this.timestamp = timestamp;
  }

  public double getCpusUsed() {
    return cpusUsed;
  }

  public double getCpusReserved() {
    return cpusReserved;
  }

  public Optional<Double> getCpusTotal() {
    return cpusTotal;
  }

  public long getMemoryBytesUsed() {
    return memoryBytesUsed;
  }

  public long getMemoryMbReserved() {
    return memoryMbReserved;
  }

  public Optional<Long> getMemoryMbTotal() {
    return memoryMbTotal.isPresent() ? Optional.of(memoryMbTotal.get()) : Optional.absent();
  }

  public Optional<Long> getMemoryBytesTotal() {
    return memoryMbTotal.isPresent() ? Optional.of(memoryMbTotal.get() * BYTES_PER_MEGABYTE) : Optional.absent();
  }

  public long getDiskBytesUsed() {
    return diskBytesUsed;
  }

  public long getDiskMbReserved() {
    return diskMbReserved;
  }

  public Optional<Long> getDiskMbTotal() {
    return diskMbTotal;
  }

  public Optional<Long> getDiskBytesTotal() {
    return diskMbTotal.isPresent() ? Optional.of(diskMbTotal.get() * BYTES_PER_MEGABYTE) : Optional.absent();
  }

  public Map<ResourceUsageType, Number> getLongRunningTasksUsage() {
    return longRunningTasksUsage;
  }

  public int getNumTasks() {
    return numTasks;
  }

  public long getTimestamp() {
    return timestamp;
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
