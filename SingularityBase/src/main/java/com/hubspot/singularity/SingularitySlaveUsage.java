package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;

/**
 * @deprecated use {@link SingularityAgentUsage}
 */
@Deprecated
public class SingularitySlaveUsage extends SingularityAgentUsage {

  public SingularitySlaveUsage(
    double cpusUsed,
    double cpusReserved,
    Optional<Double> cpusTotal,
    double memoryBytesUsed,
    double memoryMbReserved,
    Optional<Long> memoryMbTotal,
    double diskBytesUsed,
    double diskMbReserved,
    Optional<Long> diskMbTotal,
    int numTasks,
    long timestamp,
    double systemMemTotalBytes,
    double systemMemFreeBytes,
    double systemCpusTotal,
    double systemLoad1Min,
    double systemLoad5Min,
    double systemLoad15Min,
    double diskUsed,
    double diskTotal
  ) {
    super(
      cpusUsed,
      cpusReserved,
      cpusTotal,
      memoryBytesUsed,
      memoryMbReserved,
      memoryMbTotal,
      diskBytesUsed,
      diskMbReserved,
      diskMbTotal,
      numTasks,
      timestamp,
      systemMemTotalBytes,
      systemMemFreeBytes,
      systemCpusTotal,
      systemLoad1Min,
      systemLoad5Min,
      systemLoad15Min,
      null,
      null,
      diskUsed,
      diskTotal
    );
  }

  @JsonCreator
  public SingularitySlaveUsage(
    @JsonProperty("cpusUsed") double cpusUsed,
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
    @JsonProperty("slaveDiskUsed") Double slaveDiskUsed,
    @JsonProperty("slaveDiskTotal") Double slaveDiskTotal,
    @JsonProperty("diskUsed") Double diskUsed,
    @JsonProperty("diskTotal") Double diskTotal
  ) {
    super(
      cpusUsed,
      cpusReserved,
      cpusTotal,
      memoryBytesUsed,
      memoryMbReserved,
      memoryMbTotal,
      diskBytesUsed,
      diskMbReserved,
      diskMbTotal,
      numTasks,
      timestamp,
      systemMemTotalBytes,
      systemMemFreeBytes,
      systemCpusTotal,
      systemLoad1Min,
      systemLoad5Min,
      systemLoad15Min,
      slaveDiskUsed,
      slaveDiskTotal,
      diskUsed,
      diskTotal
    );
  }
}
