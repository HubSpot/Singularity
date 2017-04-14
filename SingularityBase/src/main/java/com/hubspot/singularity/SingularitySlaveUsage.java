package com.hubspot.singularity;

import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularitySlaveUsage {

  public static final String CPU_USED = "cpusUsed";
  public static final String MEMORY_BYTES_USED = "memoryRssBytes";
  public static final long BYTES_PER_MEGABYTE = 1024L * 1024L;

  private final long memoryBytesUsed;
  private final int numTasks;
  private final long timestamp;
  private final double cpusUsed;
  private final Optional<Long> memoryMbTotal;
  private final Optional<Double> cpuTotal;
  private final Map<RequestType, Map<String, Number>> usagePerRequestType;

  @JsonCreator
  public SingularitySlaveUsage(@JsonProperty("memoryBytesUsed") long memoryBytesUsed,
                               @JsonProperty("timestamp") long timestamp,
                               @JsonProperty("cpusUsed") double cpusUsed,
                               @JsonProperty("numTasks") int numTasks,
                               @JsonProperty("memoryMbTotal") Optional<Long> memoryMbTotal,
                               @JsonProperty("cpuTotal") Optional<Double> cpuTotal,
                               @JsonProperty("usagePerRequestType") Map<RequestType, Map<String, Number>> usagePerRequestType) {
    this.memoryBytesUsed = memoryBytesUsed;
    this.timestamp = timestamp;
    this.cpusUsed = cpusUsed;
    this.numTasks = numTasks;
    this.memoryMbTotal = memoryMbTotal;
    this.cpuTotal = cpuTotal;
    this.usagePerRequestType = usagePerRequestType;
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
    return memoryMbTotal.isPresent() ? Optional.of(memoryMbTotal.get() * BYTES_PER_MEGABYTE) : Optional.empty();
  }

  public Optional<Long> getMemoryMbTotal() {
    return memoryMbTotal.isPresent() ? Optional.of(memoryMbTotal.get()) : Optional.empty();
  }

  public Optional<Double> getCpuTotal() {
    return cpuTotal;
  }

  public Map<RequestType, Map<String, Number>> getUsagePerRequestType() {
    return usagePerRequestType;
  }

  public double getCpusUsedForRequestType(RequestType type) {
    return usagePerRequestType.get(type).get(CPU_USED).doubleValue();
  }

  public long getMemBytesUsedForRequestType(RequestType type) {
    return usagePerRequestType.get(type).get(MEMORY_BYTES_USED).longValue();
  }

  @Override
  public String toString() {
    return "SingularitySlaveUsage [memoryBytesUsed=" + memoryBytesUsed + ", numTasks=" + numTasks + ", timestamp=" + timestamp + ", cpusUsed=" + cpusUsed + "]";
  }

}
