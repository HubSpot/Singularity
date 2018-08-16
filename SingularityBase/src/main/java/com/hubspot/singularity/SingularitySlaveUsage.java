package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    title = "A description of resources used on a mesos slave",
    subTypes = {SingularitySlaveUsageWithId.class}
)
public class SingularitySlaveUsage {
  public static final long BYTES_PER_MEGABYTE = 1024L * 1024L;

  private final double cpusUsed;
  private final double cpusReserved;
  private final Optional<Double> cpusTotal;
  private final double memoryBytesUsed;
  private final double memoryMbReserved;
  private final Optional<Long> memoryMbTotal;
  private final double diskBytesUsed;
  private final double diskMbReserved;
  private final Optional<Long> diskMbTotal;
  private final int numTasks;
  private final long timestamp;
  private final double systemMemTotalBytes;
  private final double systemMemFreeBytes;
  private final double systemCpusTotal;
  private final double systemLoad1Min;
  private final double systemLoad5Min;
  private final double systemLoad15Min;
  private final double slaveDiskUsed;
  private final double slaveDiskTotal;

  @JsonCreator
  public SingularitySlaveUsage(@JsonProperty("cpusUsed") double cpusUsed,
                               @JsonProperty("cpusReserved") double cpusReserved,
                               @JsonProperty("cpusTotal") Optional<Double> cpusTotal,
                               @JsonProperty("memoryBytesUsed") double memoryBytesUsed,
                               @JsonProperty("memoryMbReserved") double memoryMbReserved,
                               @JsonProperty("memoryMbTotal") Optional<Long> memoryMbTotal,
                               @JsonProperty("diskBytesUsed") double diskBytesUsed,
                               @JsonProperty("diskMbReserved") double diskMbReserved,
                               @JsonProperty("diskMbTotal") Optional<Long> diskMbTotal,
                               @JsonProperty("numTasks") int numTasks,
                               @JsonProperty("timestamp") long timestamp,
                               @JsonProperty("systemMemTotalBytes") double systemMemTotalBytes,
                               @JsonProperty("systemMemFreeBytes") double systemMemFreeBytes,
                               @JsonProperty("systemCpusTotal") double systemCpusTotal,
                               @JsonProperty("systemLoad1Min") double systemLoad1Min,
                               @JsonProperty("systemLoad5Min") double systemLoad5Min,
                               @JsonProperty("systemLoad15Min") double systemLoad15Min,
                               @JsonProperty("slaveDiskUsed") double slaveDiskUsed,
                               @JsonProperty("slaveDiskTotal") double slaveDiskTotal) {
    this.cpusUsed = cpusUsed;
    this.cpusReserved = cpusReserved;
    this.cpusTotal = cpusTotal;
    this.memoryBytesUsed = memoryBytesUsed;
    this.memoryMbReserved = memoryMbReserved;
    this.memoryMbTotal = memoryMbTotal;
    this.diskBytesUsed = diskBytesUsed;
    this.diskMbReserved = diskMbReserved;
    this.diskMbTotal = diskMbTotal;
    this.numTasks = numTasks;
    this.timestamp = timestamp;
    this.systemMemTotalBytes = systemMemTotalBytes;
    this.systemMemFreeBytes = systemMemFreeBytes;
    this.systemCpusTotal = systemCpusTotal;
    this.systemLoad1Min = systemLoad1Min;
    this.systemLoad5Min = systemLoad5Min;
    this.systemLoad15Min = systemLoad15Min;
    this.slaveDiskUsed = slaveDiskUsed;
    this.slaveDiskTotal = slaveDiskTotal;
  }

  @Schema(description = "Total cpus used by tasks")
  public double getCpusUsed() {
    return cpusUsed;
  }

  @Schema(description = "Total cpus reserved by tasks")
  public double getCpusReserved() {
    return cpusReserved;
  }

  @Schema(
      title = "Total cpus available to allocate",
      description = "If oversubscribing a resource, this is the oversubscribed value, not the true value",
      nullable = true
  )
  public Optional<Double> getCpusTotal() {
    return cpusTotal;
  }

  @Schema(description = "Total memory used by tasks in bytes")
  public double getMemoryBytesUsed() {
    return memoryBytesUsed;
  }

  @Schema(description = "Total memory reserved by tasks in MB")
  public double getMemoryMbReserved() {
    return memoryMbReserved;
  }

  @Schema(
      title = "Total memory available to allocate in bytes",
      description = "If oversubscribing a resource, this is the oversubscribed value, not the true value",
      nullable = true
  )
  public Optional<Long> getMemoryMbTotal() {
    return memoryMbTotal.isPresent() ? Optional.of(memoryMbTotal.get()) : Optional.absent();
  }

  @Schema(
      title = "Total memory available to allocate in bytes",
      description = "If oversubscribing a resource, this is the oversubscribed value, not the true value",
      nullable = true
  )
  public Optional<Long> getMemoryBytesTotal() {
    return memoryMbTotal.isPresent() ? Optional.of(memoryMbTotal.get() * BYTES_PER_MEGABYTE) : Optional.absent();
  }

  @Schema(description = "Total disk currently used by tasks in bytes")
  public double getDiskBytesUsed() {
    return diskBytesUsed;
  }

  @Schema(description = "Total disk currently reserved by tasks in MB")
  public double getDiskMbReserved() {
    return diskMbReserved;
  }

  @Schema(
      title = "Total disk available to allocate in MB",
      description = "If oversubscribing a resource, this is the oversubscribed value, not the true value",
      nullable = true
  )
  public Optional<Long> getDiskMbTotal() {
    return diskMbTotal;
  }

  @Schema(
      title = "Total disk available to allocate in bytes",
      description = "If oversubscribing a resource, this is the oversubscribed value, not the true value",
      nullable = true
  )
  public Optional<Long> getDiskBytesTotal() {
    return diskMbTotal.isPresent() ? Optional.of(diskMbTotal.get() * BYTES_PER_MEGABYTE) : Optional.absent();
  }

  @Schema(description = "Number of active tasks on this salve")
  public int getNumTasks() {
    return numTasks;
  }

  @Schema(description = "Timestamp when usage data was collected")
  public long getTimestamp() {
    return timestamp;
  }

  @Schema(description = "Total memory in bytes")
  public double getSystemMemTotalBytes() {
    return systemMemTotalBytes;
  }

  @Schema(description = "Free memory in bytes")
  public double getSystemMemFreeBytes() {
    return systemMemFreeBytes;
  }

  @Schema(description = "Number of CPUs available in this slave node")
  public double getSystemCpusTotal() {
    return systemCpusTotal;
  }

  @Schema(description = "Load average for the past minute")
  public double getSystemLoad1Min() {
    return systemLoad1Min;
  }

  @Schema(description = "Load average for the past 5 minutes")
  public double getSystemLoad5Min() {
    return systemLoad5Min;
  }

  @Schema(description = "Load average for the past 15 minutes")
  public double getSystemLoad15Min() {
    return systemLoad15Min;
  }

  @Schema(
      title = "Total disk space used on the slave in bytes",
      description = "Disk usage is only collected when disk enforcement is enable and disk resources for a task are > 0"
  )
  public double getSlaveDiskUsed() {
    return slaveDiskUsed;
  }

  @Schema(description = "Total disk spave available on the slave in bytes")
  public double getSlaveDiskTotal() {
    return slaveDiskTotal;
  }

  @Override
  public String toString() {
    return "SingularitySlaveUsage{" +
        "cpusUsed=" + cpusUsed +
        ", cpusReserved=" + cpusReserved +
        ", cpusTotal=" + cpusTotal +
        ", memoryBytesUsed=" + memoryBytesUsed +
        ", memoryMbReserved=" + memoryMbReserved +
        ", memoryMbTotal=" + memoryMbTotal +
        ", diskBytesUsed=" + diskBytesUsed +
        ", diskMbReserved=" + diskMbReserved +
        ", diskMbTotal=" + diskMbTotal +
        ", numTasks=" + numTasks +
        ", timestamp=" + timestamp +
        ", systemMemTotalBytes=" + systemMemTotalBytes +
        ", systemMemFreeBytes=" + systemMemFreeBytes +
        ", systemCpusTotal=" + systemCpusTotal +
        ", systemLoad1Min=" + systemLoad1Min +
        ", systemLoad5Min=" + systemLoad5Min +
        ", systemLoad15Min=" + systemLoad15Min +
        ", slaveDiskUsed=" + slaveDiskUsed +
        ", slaveDiskTotal=" + slaveDiskTotal +
        '}';
  }
}
