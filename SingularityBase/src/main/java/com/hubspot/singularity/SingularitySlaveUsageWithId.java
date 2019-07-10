package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A description of resources used on a mesos slave")
public class SingularitySlaveUsageWithId extends SingularitySlaveUsage {

  private final String slaveId;

  public SingularitySlaveUsageWithId(SingularitySlaveUsage usage, String slaveId) {
    super(
        usage.getCpusUsed(),
        usage.getCpusReserved(),
        usage.getCpusTotal(),
        usage.getMemoryBytesUsed(),
        usage.getMemoryMbReserved(),
        usage.getMemoryMbTotal(),
        usage.getDiskBytesUsed(),
        usage.getDiskMbReserved(),
        usage.getDiskMbTotal(),
        usage.getNumTasks(),
        usage.getTimestamp(),
        usage.getSystemMemTotalBytes(),
        usage.getSystemMemFreeBytes(),
        usage.getSystemCpusTotal(),
        usage.getSystemLoad1Min(),
        usage.getSystemLoad5Min(),
        usage.getSystemLoad15Min(),
        usage.getSlaveDiskUsed(),
        usage.getSlaveDiskTotal()
    );
    this.slaveId = slaveId;
  }

  @JsonCreator
  public SingularitySlaveUsageWithId(@JsonProperty("cpusUsed") double cpusUsed,
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
                                     @JsonProperty("slaveId") String slaveId) {
    super(cpusUsed, cpusReserved, cpusTotal, memoryBytesUsed, memoryMbReserved, memoryMbTotal,
        diskBytesUsed, diskMbReserved, diskMbTotal, numTasks, timestamp, systemMemTotalBytes, systemMemFreeBytes,
        systemCpusTotal, systemLoad1Min, systemLoad5Min, systemLoad15Min, slaveDiskUsed, slaveDiskTotal);
    this.slaveId = slaveId;
  }

  @Schema(description = "The id as assigned by mesos for this particualr slave")
  public String getSlaveId() {
    return slaveId;
  }

  @Override
  public String toString() {
    return "SingularitySlaveUsageWithId [slaveId=" + slaveId + ", super=" + super.toString() + "]";
  }



}
