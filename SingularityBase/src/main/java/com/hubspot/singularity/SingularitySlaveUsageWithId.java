package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;

/**
 * @deprecated use {@link SingularityAgentUsageWithId}
 */
@Deprecated
public class SingularitySlaveUsageWithId extends SingularityAgentUsageWithId {

  @JsonCreator
  public SingularitySlaveUsageWithId(
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
    @JsonProperty("slaveDiskUsed") double slaveDiskUsed,
    @JsonProperty("slaveDiskTotal") double slaveDiskTotal,
    @JsonProperty("slaveId") String slaveId,
    @JsonProperty("agentId") String agentId
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
      slaveId,
      agentId
    );
  }
}
